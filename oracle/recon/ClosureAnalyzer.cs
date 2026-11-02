using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Mono.Cecil;

class ClosureAnalyzer
{
    static AssemblyDefinition baseAsm, mageAsm;
    static Dictionary<string, TypeDefinition> baseTypes = new Dictionary<string, TypeDefinition>();
    static HashSet<string> neededTypes = new HashSet<string>();
    static HashSet<string> neededMembers = new HashSet<string>();
    static Dictionary<string, HashSet<string>> typeMembers = new Dictionary<string, HashSet<string>>();
    static Queue<string> typeQueue = new Queue<string>();
    static Queue<string> memberQueue = new Queue<string>();
    static Dictionary<string, string> typeReason = new Dictionary<string, string>();
    static Dictionary<string, string> memberReason = new Dictionary<string, string>();
    static string ctx = "seed";
    static HashSet<string> skipTypes = new HashSet<string>();
    static List<string> prunePrefixes = new List<string>();

    static bool IsUiMethod(MethodDefinition m)
    {
        var scopes = new HashSet<string>();
        CollectScopes(m.ReturnType, scopes);
        foreach (var p in m.Parameters) CollectScopes(p.ParameterType, scopes);
        if (m.HasCustomAttributes)
            foreach (var ca in m.CustomAttributes) CollectScopes(ca.AttributeType, scopes);
        if (m.HasBody)
            foreach (var ins in m.Body.Instructions)
            {
                var mem = ins.Operand as MemberReference;
                if (mem != null) CollectScopes(mem.DeclaringType, scopes);
                var tr = ins.Operand as TypeReference;
                if (tr != null) CollectScopes(tr, scopes);
            }
        return scopes.Any(s => s == "System.Windows.Forms" || s == "System.Drawing" || s == "System.Windows" || s == "System.Windows.Controls" || s == "System.Web");
    }

    static bool IsBase(TypeReference t)
    {
        if (t == null) return false;
        var s = t.Scope;
        return s != null && (s.Name == "Rawr.Base" || s.Name == "Rawr.Base.dll");
    }

    static void AddType(TypeReference t)
    {
        if (t == null) return;
        t = t.GetElementType();
        if (!IsBase(t)) return;
        string fn = t.FullName;
        if (neededTypes.Add(fn))
        {
            typeQueue.Enqueue(fn);
            typeReason[fn] = ctx;
            if (!typeMembers.ContainsKey(fn)) typeMembers[fn] = new HashSet<string>();
        }
    }

    static void AddMember(MethodReference m)
    {
        if (m == null || !IsBase(m.DeclaringType)) return;
        string key = m.DeclaringType.GetElementType().FullName + "::" + m.Name + "(" + m.Parameters.Count + ")";
        AddType(m.DeclaringType);
        if (neededMembers.Add(key))
        {
            memberQueue.Enqueue(key);
            memberReason[key] = ctx;
            typeMembers[m.DeclaringType.GetElementType().FullName].Add(key);
        }
    }

    static void AddMember(FieldReference f)
    {
        if (f == null || !IsBase(f.DeclaringType)) return;
        string key = f.DeclaringType.GetElementType().FullName + "::" + f.Name;
        AddType(f.DeclaringType);
        if (neededMembers.Add(key))
        {
            memberQueue.Enqueue(key);
            memberReason[key] = ctx;
            typeMembers[f.DeclaringType.GetElementType().FullName].Add(key);
        }
    }

    static IEnumerable<TypeReference> WalkType(TypeReference t)
    {
        if (t == null) yield break;
        yield return t;
        var spec = t as TypeSpecification;
        if (spec != null)
            foreach (var x in WalkType(spec.ElementType)) yield return x;
        var gi = t as GenericInstanceType;
        if (gi != null)
            foreach (var a in gi.GenericArguments)
                foreach (var x in WalkType(a)) yield return x;
    }

    static void ScanBody(MethodDefinition def)
    {
        if (!def.HasBody) return;
        foreach (var ins in def.Body.Instructions)
        {
            var op = ins.Operand;
            var mr = op as MethodReference;
            if (mr != null) { AddMember(mr); continue; }
            var fr = op as FieldReference;
            if (fr != null) { AddMember(fr); foreach (var x in WalkType(fr.FieldType)) AddType(x); continue; }
            var tr = op as TypeReference;
            if (tr != null) { foreach (var x in WalkType(tr)) AddType(x); continue; }
        }
        foreach (var v in def.Body.Variables)
            foreach (var x in WalkType(v.VariableType)) AddType(x);
    }

    static void ScanSignature(MethodDefinition def)
    {
        foreach (var x in WalkType(def.ReturnType)) AddType(x);
        foreach (var p in def.Parameters)
            foreach (var x in WalkType(p.ParameterType)) AddType(x);
    }

    static void ProcessType(string fn)
    {
        TypeDefinition td;
        if (!baseTypes.TryGetValue(fn, out td)) return;
        ctx = "type:" + fn;
        ScanAttributes(td);
        foreach (var x in WalkType(td.BaseType)) AddType(x);
        foreach (var i in td.Interfaces)
            foreach (var x in WalkType(i.InterfaceType)) AddType(x);
        if (td.DeclaringType != null) AddType(td.DeclaringType);
        if (td.IsEnum)
            foreach (var f in td.Fields) AddMember(f);
        var cc = td.Methods.FirstOrDefault(m => m.Name == ".cctor");
        if (cc != null) AddMember(cc);
    }

    static MethodDefinition FindMethod(string key)
    {
        int sep = key.IndexOf("::");
        string tn = key.Substring(0, sep);
        string rest = key.Substring(sep + 2);
        int paren = rest.IndexOf('(');
        string name = rest.Substring(0, paren);
        int argc = int.Parse(rest.Substring(paren + 1, rest.Length - paren - 2));
        TypeDefinition td;
        if (!baseTypes.TryGetValue(tn, out td)) return null;
        return td.Methods.FirstOrDefault(m => m.Name == name && m.Parameters.Count == argc);
    }

    static FieldDefinition FindField(string key)
    {
        int sep = key.IndexOf("::");
        string tn = key.Substring(0, sep);
        string name = key.Substring(sep + 2);
        TypeDefinition td;
        if (!baseTypes.TryGetValue(tn, out td)) return null;
        return td.Fields.FirstOrDefault(f => f.Name == name);
    }

    static void ScanAttributes(ICustomAttributeProvider provider)
    {
        if (provider == null || !provider.HasCustomAttributes) return;
        foreach (var ca in provider.CustomAttributes)
        {
            foreach (var x in WalkType(ca.AttributeType)) AddType(x);
            if (ca.HasConstructorArguments)
                foreach (var arg in ca.ConstructorArguments)
                    foreach (var x in WalkType(arg.Type)) AddType(x);
            if (ca.HasProperties)
                foreach (var na in ca.Properties)
                    foreach (var x in WalkType(na.Argument.Type)) AddType(x);
            if (ca.HasFields)
                foreach (var fa in ca.Fields)
                    foreach (var x in WalkType(fa.Argument.Type)) AddType(x);
        }
    }

    static void ScanAssembly(AssemblyDefinition asm)
    {
        foreach (var td in AllTypes(asm.MainModule))
        {
            if (skipTypes.Contains(td.Name)) continue;
            if (td.BaseType != null && skipTypes.Contains(td.BaseType.Name)) continue;
            ctx = "mage:" + td.FullName;
            ScanAttributes(td);
            foreach (var x in WalkType(td.BaseType)) AddType(x);
            foreach (var i in td.Interfaces)
                foreach (var x in WalkType(i.InterfaceType)) AddType(x);
            foreach (var f in td.Fields)
            {
                ScanAttributes(f);
                foreach (var x in WalkType(f.FieldType)) AddType(x);
            }
            foreach (var p in td.Properties)
            {
                if (p.GetMethod != null && IsUiMethod(p.GetMethod) && p.SetMethod == null) continue;
                if (p.SetMethod != null && IsUiMethod(p.SetMethod) && p.GetMethod == null) continue;
                ScanAttributes(p);
                foreach (var x in WalkType(p.PropertyType)) AddType(x);
                foreach (var pa in p.Parameters)
                    foreach (var x in WalkType(pa.ParameterType)) AddType(x);
            }
            foreach (var e in td.Events)
            {
                ScanAttributes(e);
                foreach (var x in WalkType(e.EventType)) AddType(x);
            }
            foreach (var m in td.Methods)
            {
                if (IsUiMethod(m)) continue;
                ctx = "mage:" + td.FullName + "::" + m.Name;
                ScanAttributes(m);
                foreach (var x in WalkType(m.ReturnType)) AddType(x);
                foreach (var p in m.Parameters)
                {
                    ScanAttributes(p);
                    foreach (var x in WalkType(p.ParameterType)) AddType(x);
                }
                ScanBody(m);
            }
        }
    }

    static IEnumerable<TypeDefinition> AllTypes(ModuleDefinition mod)
    {
        foreach (var t in mod.Types)
        {
            yield return t;
            foreach (var n in AllNested(t)) yield return n;
        }
    }

    static IEnumerable<TypeDefinition> AllNested(TypeDefinition t)
    {
        foreach (var n in t.NestedTypes)
        {
            yield return n;
            foreach (var x in AllNested(n)) yield return x;
        }
    }

    static bool IsUiType(TypeDefinition td)
    {
        var scopes = new HashSet<string>();
        foreach (var f in td.Fields) CollectScopes(f.FieldType, scopes);
        foreach (var m in td.Methods)
        {
            CollectScopes(m.ReturnType, scopes);
            foreach (var p in m.Parameters) CollectScopes(p.ParameterType, scopes);
            if (m.HasBody)
            {
                foreach (var ins in m.Body.Instructions)
                {
                    var op = ins.Operand as MemberReference;
                    if (op != null) CollectScopes(op.DeclaringType, scopes);
                    var tr = ins.Operand as TypeReference;
                    if (tr != null) CollectScopes(tr, scopes);
                }
            }
        }
        foreach (var i in td.Interfaces) CollectScopes(i.InterfaceType, scopes);
        if (td.BaseType != null) CollectScopes(td.BaseType, scopes);
        return scopes.Any(s => s == "System.Windows.Forms" || s == "System.Drawing" || s == "System.Windows" || s == "System.Web");
    }

    static void CollectScopes(TypeReference t, HashSet<string> scopes)
    {
        foreach (var x in WalkType(t))
        {
            if (x.Scope != null) scopes.Add(x.Scope.Name);
            var gi = x as GenericInstanceType;
            if (gi != null)
                foreach (var a in gi.GenericArguments) CollectScopes(a, scopes);
        }
    }

    static void Main(string[] args)
    {
        string binDir = args[0];
        if (args.Length > 1)
            foreach (var s in args[1].Split(',')) if (s.Length > 0) skipTypes.Add(s);
        if (args.Length > 2)
            foreach (var s in args[2].Split(',')) if (s.Length > 0) prunePrefixes.Add(s);
        var resolver = new DefaultAssemblyResolver();
        resolver.AddSearchDirectory(binDir);
        var rp = new ReaderParameters { AssemblyResolver = resolver };
        baseAsm = AssemblyDefinition.ReadAssembly(Path.Combine(binDir, "Rawr.Base.dll"), rp);
        mageAsm = AssemblyDefinition.ReadAssembly(Path.Combine(binDir, "Rawr.Mage.dll"), rp);
        foreach (var t in AllTypes(baseAsm.MainModule)) baseTypes[t.FullName] = t;

        ScanAssembly(mageAsm);

        int guard = 0;
        while ((typeQueue.Count > 0 || memberQueue.Count > 0) && guard < 1000000)
        {
            guard++;
            while (typeQueue.Count > 0) ProcessType(typeQueue.Dequeue());
            while (memberQueue.Count > 0)
            {
                string key = memberQueue.Dequeue();
                ctx = "member:" + key;
                if (prunePrefixes.Any(p => key.StartsWith(p)))
                {
                    var pd = FindMethod(key);
                    if (pd != null) ScanSignature(pd);
                    continue;
                }
                if (key.Contains("("))
                {
                    var def = FindMethod(key);
                    if (def != null) { ScanAttributes(def); ScanSignature(def); ScanBody(def); }
                }
                else
                {
                    var def = FindField(key);
                    if (def != null)
                    {
                        ScanAttributes(def);
                        foreach (var x in WalkType(def.FieldType)) AddType(x);
                        var td = def.DeclaringType;
                        if (def.IsStatic) { var cc = td.Methods.FirstOrDefault(m => m.Name == ".cctor"); if (cc != null) AddMember(cc); }
                    }
                }
            }
        }

        Console.Out.WriteLine("NEEDED_TYPES " + neededTypes.Count);
        Console.Out.WriteLine("NEEDED_MEMBERS " + neededMembers.Count);
        Console.Out.WriteLine("TYPES_WITHOUT_DEF " + neededTypes.Count(t => !baseTypes.ContainsKey(t)));
        foreach (var fn in neededTypes.OrderBy(x => x))
        {
            TypeDefinition td = baseTypes.ContainsKey(fn) ? baseTypes[fn] : null;
            bool ui = td != null && IsUiType(td);
            Console.Out.WriteLine("T\t" + fn + "\t" + (ui ? "UI" : "-") + "\t" + (td == null ? "?" : td.Methods.Count + "/" + td.Fields.Count));
        }
        foreach (var m in neededMembers.OrderBy(x => x))
            Console.Out.WriteLine("M\t" + m + "\t" + (memberReason.ContainsKey(m) ? memberReason[m] : "?"));
        foreach (var kv in typeReason.OrderBy(x => x.Key))
            Console.Out.WriteLine("R\t" + kv.Key + "\t" + kv.Value);
    }
}
