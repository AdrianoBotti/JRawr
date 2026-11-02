// CoreCompleteness — member-level audit of Rawr.Base.dll against the Java port.
//
// Enumerates every public/protected type and member of the prebuilt Rawr.Base.dll (the shipped
// app's assembly, C# namespace "Rawr"), together with evidence for reachability:
//   * IL references from every shipped Rawr.*.dll / Rawr.exe (call/callvirt/newobj/ldfld/...),
//   * interface dispatch (implicit interface implementations are called through the interface),
//   * reflection-ish string literals (exact member name / Type.Member or Type::Member substring).
// Classification helpers (UI namespaces, network types, designer members) are computed here so the
// Python matcher only has to join against the Java sources.
//
// Build (Mono.Cecil 0.11.1, as oracle/recon):
//   mcs -langversion:default \
//     -r:/usr/lib/mono/gac/Mono.Cecil/0.11.1.0__0738eb9f132ed756/Mono.Cecil.dll \
//     -out:out/CoreCompleteness.exe CoreCompleteness.cs
// Run:
//   mono out/CoreCompleteness.exe <bin/Release Data dir> out/dll-members.tsv
//
// Output TSV, tab separated:
//   T <fullname> <visibility> <flags>
//   M <type> <kind> <name> <arity> <visibility> <static> <reach> <detail>
// kinds: method ctor field property event
// reach: used-self | used-shipped | virtual | iface | refl | dead
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Mono.Cecil;
using Mono.Cecil.Cil;

class CoreCompleteness
{
    const string BaseName = "Rawr.Base";

    static AssemblyDefinition baseAsm;
    static readonly List<AssemblyDefinition> shipped = new List<AssemblyDefinition>();
    static readonly HashSet<string> referenced = new HashSet<string>();
    static readonly HashSet<string> refAssemblies = new HashSet<string>();
    static readonly HashSet<string> stringLiterals = new HashSet<string>();
    static readonly Dictionary<string, string> selfRef = new Dictionary<string, string>();
    static readonly Dictionary<string, string> shippedRef = new Dictionary<string, string>();
    static readonly HashSet<string> externalRef = new HashSet<string>();
    static readonly Dictionary<string, HashSet<string>> edges = new Dictionary<string, HashSet<string>>();
    static readonly Dictionary<string, List<string>> baseToImpls = new Dictionary<string, List<string>>();
    static readonly Dictionary<string, List<string>> implToBases = new Dictionary<string, List<string>>();
    static readonly Dictionary<string, string> typeCctor = new Dictionary<string, string>();
    static HashSet<string> live = new HashSet<string>();

    static readonly HashSet<string> UiNamespaces = new HashSet<string>
    {
        "System.Windows.Forms", "System.Drawing", "System.Windows", "System.Web",
        "System.Windows.Controls", "System.Windows.Media", "System.Windows.Browser"
    };

    static readonly HashSet<string> NetworkNamespaces = new HashSet<string>
    {
        "System.Net", "System.Net.Sockets", "System.Net.Security", "System.Web"
    };

    static bool IsBase(TypeReference t)
    {
        if (t == null) return false;
        var s = t.Scope;
        if (s == null) return false;
        return s.Name == BaseName || s.Name == BaseName + ".dll";
    }

    static string NormType(TypeReference t)
    {
        if (t == null) return "";
        return t.GetElementType().FullName;
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

    static string TypeVisibility(TypeDefinition td)
    {
        if (td.IsPublic) return "public";
        if (td.IsNestedPublic) return "public";
        if (td.IsNestedFamily) return "protected";
        if (td.IsNestedFamilyOrAssembly) return "protected-internal";
        if (td.IsNestedAssembly) return "internal";
        return "private";
    }

    static string MemberVisibility(MethodDefinition m)
    {
        if (m.IsPublic) return "public";
        if (m.IsFamily) return "protected";
        if (m.IsFamilyOrAssembly) return "protected-internal";
        if (m.IsAssembly) return "internal";
        return "private";
    }

    static string MemberVisibility(FieldDefinition f)
    {
        if (f.IsPublic) return "public";
        if (f.IsFamily) return "protected";
        if (f.IsFamilyOrAssembly) return "protected-internal";
        if (f.IsAssembly) return "internal";
        return "private";
    }

    static bool VisibleType(TypeDefinition td)
    {
        return td.IsPublic || td.IsNestedPublic || td.IsNestedFamily || td.IsNestedFamilyOrAssembly;
    }

    static bool Visible(MethodDefinition m)
    {
        return m.IsPublic || m.IsFamily || m.IsFamilyOrAssembly;
    }

    static bool Visible(FieldDefinition f)
    {
        return f.IsPublic || f.IsFamily || f.IsFamilyOrAssembly;
    }

    static bool HasAttr(ICustomAttributeProvider p, string name)
    {
        if (!p.HasCustomAttributes) return false;
        return p.CustomAttributes.Any(a => a.AttributeType.Name == name);
    }

    static bool IsCompilerGenerated(ICustomAttributeProvider p, string name)
    {
        if (HasAttr(p, "CompilerGeneratedAttribute")) return true;
        return name != null && (name.Contains("<") || name.Contains(">"));
    }

    static bool IsDelegate(TypeDefinition td)
    {
        return td.BaseType != null && td.BaseType.FullName == "System.MulticastDelegate";
    }

    // ---- IL scanning -------------------------------------------------------

    static string RefKey(MemberReference m)
    {
        if (m == null) return null;
        var dt = m.DeclaringType;
        if (dt == null) return null;
        if (m is MethodReference)
            return NormType(dt) + "::" + m.Name + "(" + ((MethodReference)m).Parameters.Count + ")";
        return NormType(dt) + "::" + m.Name;
    }

    static void AddRef(MemberReference m, bool isBase, HashSet<string> refs)
    {
        string key = RefKey(m);
        if (key == null) return;
        referenced.Add(key);
        if (!shippedRef.ContainsKey(key)) shippedRef[key] = "sig";
        if (isBase)
        {
            if (!selfRef.ContainsKey(key)) selfRef[key] = "sig";
            if (refs != null) refs.Add(key);
        }
        else
        {
            externalRef.Add(key);
        }
    }

    static string MethodKey(MethodDefinition m)
    {
        return m.DeclaringType.FullName + "::" + m.Name + "(" + m.Parameters.Count + ")";
    }

    static void ScanAssembly(AssemblyDefinition asm, bool isBase)
    {
        foreach (var td in AllTypes(asm.MainModule))
        {
            foreach (var m in td.Methods)
            {
                if (!m.HasBody) continue;
                HashSet<string> refs = null;
                if (isBase)
                {
                    refs = new HashSet<string>();
                    edges[MethodKey(m)] = refs;
                }
                foreach (var ins in m.Body.Instructions)
                {
                    if (ins.OpCode == OpCodes.Ldstr)
                    {
                        var s = ins.Operand as string;
                        if (s != null && s.Length > 0 && s.Length < 200) stringLiterals.Add(s);
                        continue;
                    }
                    var op = ins.Operand;
                    var mr = op as MethodReference;
                    if (mr != null) { AddRef(mr, isBase, refs); continue; }
                    var fr = op as FieldReference;
                    if (fr != null)
                    {
                        AddRef(fr, isBase, refs);
                        continue;
                    }
                    var tr = op as TypeReference;
                    if (tr != null)
                    {
                        var key = "T:" + NormType(tr);
                        referenced.Add(key);
                        shippedRef[key] = "body";
                        if (isBase) selfRef[key] = "body";
                    }
                }
            }
            // custom attributes in signatures are metadata, not dispatch; strings above cover reflection.
        }
    }

    static bool ReflectionMention(string typeName, string memberName)
    {
        if (memberName == null || memberName.Length < 6) return false;
        if (memberName != memberName.ToUpperInvariant() && !memberName.Any(char.IsUpper)) return false;
        if (stringLiterals.Contains(memberName)) return true;
        string simple = typeName.Contains("/") ? typeName.Substring(typeName.LastIndexOf('/') + 1) : typeName;
        return stringLiterals.Contains(simple + "." + memberName)
            || stringLiterals.Contains(simple + "::" + memberName)
            || stringLiterals.Contains(typeName + "." + memberName);
    }

    // ---- interface dispatch ------------------------------------------------

    static readonly Dictionary<string, List<TypeDefinition>> allByFull = new Dictionary<string, List<TypeDefinition>>();

    static string ImplKey(MethodReference m)
    {
        return m.Name + "(" + m.Parameters.Count + ")";
    }

    // Interface dispatch: map interface member keys to their implementations (implicit name/arity
    // match, including base interfaces) and vice versa. Implementations of interfaces declared
    // outside Rawr.Base are seeded as live because framework code can dispatch to them.
    static void CollectInterfaceImpls(HashSet<string> ifaceImpls, List<string> externalIfaceSeeds)
    {
        foreach (var td in AllTypes(baseAsm.MainModule))
        {
            foreach (var iface in td.Interfaces)
            {
                var it = iface.InterfaceType;
                TypeDefinition itd = null;
                var key = NormType(it);
                List<TypeDefinition> list;
                if (allByFull.TryGetValue(key, out list)) itd = list[0];
                var ifaceMethods = new List<string>();
                CollectIfaceMethods(itd, new HashSet<string>(), ifaceMethods);
                bool external = itd == null;
                foreach (var m in td.Methods)
                {
                    if (!m.IsVirtual && !m.IsPublic && !m.IsFamily && !m.IsAssembly) continue;
                    string mk = ImplKey(m);
                    string dk = td.FullName + "::" + mk;
                    if (ifaceMethods.Contains(mk))
                    {
                        ifaceImpls.Add(dk);
                        foreach (var ik in ifaceMethods.Where(x => x == mk))
                        {
                            string ikkey = NormType(it) + "::" + ik;
                            AddMap(baseToImpls, ikkey, dk);
                            AddMap(implToBases, dk, ikkey);
                        }
                        if (external) externalIfaceSeeds.Add(dk);
                    }
                    // explicit implementations
                    if (m.HasOverrides)
                        foreach (var o in m.Overrides)
                        {
                            string ok = NormType(o.DeclaringType) + "::" + ImplKey(o);
                            ifaceImpls.Add(dk);
                            AddMap(baseToImpls, ok, dk);
                            AddMap(implToBases, dk, ok);
                        }
                }
            }
        }
    }

    static void CollectIfaceMethods(TypeDefinition itd, HashSet<string> seen, List<string> outKeys)
    {
        if (itd == null || !seen.Add(itd.FullName)) return;
        foreach (var im in itd.Methods) outKeys.Add(ImplKey(im));
        foreach (var b in itd.Interfaces)
        {
            List<TypeDefinition> bl;
            if (allByFull.TryGetValue(NormType(b.InterfaceType), out bl)) CollectIfaceMethods(bl[0], seen, outKeys);
        }
    }

    static void AddMap(Dictionary<string, List<string>> map, string key, string value)
    {
        List<string> l;
        if (!map.TryGetValue(key, out l)) { l = new List<string>(); map[key] = l; }
        if (!l.Contains(value)) l.Add(value);
    }

    static void BuildOverrideIndex()
    {
        foreach (var td in AllTypes(baseAsm.MainModule))
        {
            if (td.BaseType == null) continue;
            // explicit methodimpl targets (interfaces and reabstractions)
            foreach (var dm in td.Methods)
            {
                if (!dm.HasOverrides) continue;
                foreach (var o in dm.Overrides)
                {
                    string ok = NormType(o.DeclaringType) + "::" + ImplKey(o);
                    if (NormType(o.DeclaringType) != td.FullName)
                        AddMap(baseToImpls, ok, td.FullName + "::" + ImplKey(dm));
                }
            }
            // implicit virtual overrides: walk the base chain for a same-signature virtual method
            foreach (var dm in td.Methods)
            {
                if (!dm.IsVirtual || dm.IsConstructor || dm.IsGetter || dm.IsSetter) continue;
                if (dm.Overrides != null && dm.Overrides.Count > 0) continue;
                var bt = td.BaseType;
                int guard = 0;
                while (bt != null && guard++ < 32)
                {
                    List<TypeDefinition> bases;
                    if (!allByFull.TryGetValue(NormType(bt), out bases)) break;
                    var b = bases[0];
                    var bm = b.Methods.FirstOrDefault(x => x.Name == dm.Name && x.Parameters.Count == dm.Parameters.Count && (x.IsVirtual || x.IsAbstract));
                    if (bm != null)
                    {
                        AddMap(baseToImpls, b.FullName + "::" + ImplKey(bm), td.FullName + "::" + ImplKey(dm));
                        break;
                    }
                    bt = b.BaseType;
                }
            }
        }
    }

    static HashSet<string> ComputeLive(HashSet<string> ifaceImpls, List<string> externalIfaceSeeds)
    {
        var liveSet = new HashSet<string>();
        var queue = new Queue<string>();
        Action<string> seed = k =>
        {
            if (!string.IsNullOrEmpty(k) && liveSet.Add(k)) queue.Enqueue(k);
        };
        foreach (var k in externalRef) seed(k);
        foreach (var k in externalIfaceSeeds) seed(k);
        foreach (var td in AllTypes(baseAsm.MainModule))
        {
            if (ReflectionMention(td.FullName, td.Name)) seed("T:" + td.FullName);
            foreach (var m in td.Methods)
                if (Visible(m) && !IsCompilerGenerated(m, m.Name) && ReflectionMention(td.FullName, m.Name)) seed(MethodKey(m));
            foreach (var f in td.Fields)
                if (Visible(f) && !IsCompilerGenerated(f, f.Name) && ReflectionMention(td.FullName, f.Name)) seed(td.FullName + "::" + f.Name);
            foreach (var ev in td.Events)
                if (ReflectionMention(td.FullName, ev.Name) && ev.AddMethod != null) seed(MethodKey(ev.AddMethod));
            foreach (var p in td.Properties)
                if (ReflectionMention(td.FullName, p.Name) && p.GetMethod != null) seed(MethodKey(p.GetMethod));
        }
        while (queue.Count > 0)
        {
            var k = queue.Dequeue();
            List<string> impls;
            if (baseToImpls.TryGetValue(k, out impls))
                foreach (var i in impls) seed(i);
            int sep = k.IndexOf("::");
            if (sep > 0)
            {
                string cc;
                if (typeCctor.TryGetValue(k.Substring(0, sep), out cc)) seed(cc);
            }
            HashSet<string> refs;
            if (edges.TryGetValue(k, out refs))
                foreach (var r in refs) seed(r);
        }
        return liveSet;
    }

    // ---- classification ----------------------------------------------------

    static bool UsesUiNamespace(TypeReference t)
    {
        if (t == null) return false;
        var scope = t.Scope;
        if (scope != null && UiNamespaces.Contains(scope.Name)) return true;
        if (t.Namespace != null && UiNamespaces.Contains(t.Namespace)) return true;
        var gi = t as GenericInstanceType;
        if (gi != null && gi.GenericArguments.Any(UsesUiNamespace)) return true;
        return false;
    }

    static bool UsesUiNamespace(IEnumerable<TypeReference> ts)
    {
        return ts.Any(UsesUiNamespace);
    }

    static readonly HashSet<string> KnownUiTypes = new HashSet<string>
    {
        "Rawr.Base.ErrorBox", "Rawr.Base.Graph", "Rawr.Base.Graph/Style", "Rawr.StatGraphRenderer",
        "Rawr.CustomControls.ExtendedToolTipCheckBox", "Rawr.CustomControls.ExtendedToolTipLabel",
        "Rawr.Calculations/RawrModelInfoAttribute"
    };

    static bool TypeIsUi(TypeDefinition td)
    {
        if (KnownUiTypes.Contains(td.FullName)) return true;
        if (td.Namespace != null && td.Namespace.StartsWith("Rawr.Properties")) return true;
        if (td.FullName.Contains("/DG_Boss")) return true;
        if (td.Name.StartsWith("DG_")) return true;
        // UI container detection: only the type's own identity (base class chain), not its members.
        // Individual UI members are flagged separately so a core class with one chart method is not
        // misclassified wholesale.
        var bt = td.BaseType;
        int guard = 0;
        while (bt != null && guard++ < 16)
        {
            if (UsesUiNamespace(bt)) return true;
            List<TypeDefinition> bases;
            if (!allByFull.TryGetValue(NormType(bt), out bases)) break;
            bt = bases[0].BaseType;
        }
        return false;
    }

    static bool MemberIsUi(MethodDefinition m)
    {
        if (m.Name == "InitializeComponent" || m.Name == "Dispose" && m.Parameters.Count == 1) return true;
        if (UsesUiNamespace(m.ReturnType)) return true;
        if (UsesUiNamespace(m.Parameters.Select(p => p.ParameterType))) return true;
        if (m.HasBody)
            foreach (var ins in m.Body.Instructions)
            {
                var mem = ins.Operand as MemberReference;
                if (mem != null && UsesUiNamespace(mem.DeclaringType)) return true;
                var tr = ins.Operand as TypeReference;
                if (tr != null && !(tr is GenericInstanceType) && UsesUiNamespace(tr)) return true;
            }
        return false;
    }

    static bool MemberIsUi(FieldDefinition f)
    {
        return f.Name == "components" || UsesUiNamespace(f.FieldType);
    }

    static readonly HashSet<string> NetworkTypes = new HashSet<string>
    {
        "Rawr.Armory", "Rawr.ArmoryPet", "Rawr.Wowhead", "Rawr.WebRequestWrapper",
        "Rawr.ItemUpdater", "Rawr.JsonParser", "Rawr.SavedVariablesParser",
        "Rawr.SavedVariablesDictionary", "Rawr.CharacterProfilerData",
        "Rawr.CharacterProfilerCharacter", "Rawr.CharacterProfilerRealm",
        "Rawr.CharacterProfilerFailedImport", "Rawr.LocationFactory"
    };

    static bool IsNetworkTypeName(string full)
    {
        return NetworkTypes.Contains(full) || full.StartsWith("Rawr.Wowhead/") || full.StartsWith("Rawr.WebRequestWrapper/")
            || full.StartsWith("Rawr.ItemUpdater/") || full.StartsWith("Rawr.JsonParser/")
            || full.StartsWith("Rawr.SavedVariablesParser/") || full.StartsWith("Rawr.Armory/");
    }

    static bool MethodIsNetwork(MethodDefinition m)
    {
        var scope = m.ReturnType != null ? m.ReturnType.Scope : null;
        if (scope != null && NetworkNamespaces.Contains(scope.Name)) return true;
        if (m.HasBody)
            foreach (var ins in m.Body.Instructions)
            {
                var mem = ins.Operand as MemberReference;
                if (mem != null && mem.DeclaringType != null)
                {
                    var sc = mem.DeclaringType.Scope;
                    if (sc != null && NetworkNamespaces.Contains(sc.Name)) return true;
                    if (mem.DeclaringType.Namespace != null && NetworkNamespaces.Contains(mem.DeclaringType.Namespace)) return true;
                }
            }
        return false;
    }

    // ---- main --------------------------------------------------------------

    static void Main(string[] args)
    {
        string binDir = args[0];
        string outPath = args[1];
        var resolver = new DefaultAssemblyResolver();
        resolver.AddSearchDirectory(binDir);
        resolver.AddSearchDirectory(Path.GetDirectoryName(binDir));
        var rp = new ReaderParameters { AssemblyResolver = resolver, ReadSymbols = false };
        baseAsm = AssemblyDefinition.ReadAssembly(Path.Combine(binDir, "Rawr.Base.dll"), rp);

        foreach (var f in Directory.GetFiles(binDir, "Rawr.*.dll"))
        {
            var name = Path.GetFileNameWithoutExtension(f);
            if (name == "Rawr.Base") continue;
            try { shipped.Add(AssemblyDefinition.ReadAssembly(f, rp)); }
            catch (Exception e) { Console.Error.WriteLine("skip " + name + ": " + e.Message); }
        }
        string exe = Path.Combine(Path.GetDirectoryName(binDir), "Rawr.exe");
        if (File.Exists(exe)) shipped.Add(AssemblyDefinition.ReadAssembly(exe, rp));

        foreach (var a in shipped) { refAssemblies.Add(a.Name.Name); ScanAssembly(a, false); }
        ScanAssembly(baseAsm, true);

        foreach (var td in AllTypes(baseAsm.MainModule))
        {
            List<TypeDefinition> l;
            if (!allByFull.TryGetValue(td.FullName, out l)) { l = new List<TypeDefinition>(); allByFull[td.FullName] = l; }
            l.Add(td);
            var cc = td.Methods.FirstOrDefault(m => m.Name == ".cctor");
            if (cc != null) typeCctor[td.FullName] = MethodKey(cc);
        }
        BuildOverrideIndex();
        var ifaceImpls = new HashSet<string>();
        var externalIfaceSeeds = new List<string>();
        CollectInterfaceImpls(ifaceImpls, externalIfaceSeeds);
        live = ComputeLive(ifaceImpls, externalIfaceSeeds);

        var lines = new List<string>();
        int typeCount = 0, memberCount = 0;

        foreach (var td in AllTypes(baseAsm.MainModule).OrderBy(t => t.FullName))
        {
            if (IsCompilerGenerated(td, td.Name)) continue;
            if (td.DeclaringType != null && IsCompilerGenerated(td.DeclaringType, td.DeclaringType.Name)) continue;
            var flags = new List<string>();
            if (IsDelegate(td)) flags.Add("delegate");
            if (td.IsEnum) flags.Add("enum");
            if (td.IsInterface) flags.Add("interface");
            if (td.IsAbstract) flags.Add("abstract");
            if (td.IsSealed && !td.IsEnum) flags.Add("sealed");
            if (td.DeclaringType != null) flags.Add("nested");
            if (TypeIsUi(td)) flags.Add("ui");
            if (NetworkTypes.Contains(td.FullName)) flags.Add("network");
            bool visible = VisibleType(td);
            if (!visible) flags.Add("nonpublic");
            string tkey = "T:" + td.FullName;
            bool typeUsed = referenced.Contains(tkey) || ReflectionMention(td.FullName, td.Name);
            if (!typeUsed) flags.Add("unreferenced-type");
            typeCount++;
            lines.Add("T\t" + td.FullName + "\t" + TypeVisibility(td) + "\t" + string.Join(",", flags));

            // ---- methods
            foreach (var m in td.Methods)
            {
                if (!Visible(m)) continue;
                if (IsCompilerGenerated(m, m.Name)) continue;
                bool accessor = m.IsGetter || m.IsSetter || m.IsAddOn || m.IsRemoveOn;
                if (accessor) continue;
                if (m.IsSpecialName && !m.IsConstructor) continue;
                memberCount++;
                string key = td.FullName + "::" + m.Name + "(" + m.Parameters.Count + ")";
                string reach;
                if (!live.Contains(key)) reach = "dead";
                else if (externalRef.Contains(key)) reach = "used-shipped";
                else if (selfRef.ContainsKey(key)) reach = "used-self";
                else if (ifaceImpls.Contains(key)) reach = "iface";
                else if (m.IsVirtual || m.IsAbstract) reach = "virtual";
                else if (ReflectionMention(td.FullName, m.Name)) reach = "refl";
                else reach = "live";
                var mflags = new List<string>();
                if (m.IsStatic) mflags.Add("static");
                if (m.IsAbstract) mflags.Add("abstract");
                if (m.IsVirtual) mflags.Add("virtual");
                if (m.IsConstructor) mflags.Add("ctor");
                if (mflags.Count == 0) mflags.Add("-");
                var detail = reach == "used-shipped" ? "model-or-exe" : "-";
                bool mui = MemberIsUi(m);
                bool net = MethodIsNetwork(m);
                lines.Add("M\t" + td.FullName + "\t" + (m.IsConstructor ? "ctor" : "method") + "\t" + m.Name + "\t"
                    + m.Parameters.Count + "\t" + MemberVisibility(m) + "\t" + string.Join(",", mflags) + "\t"
                    + reach + "\t" + detail + (mui ? ",ui" : "") + (net ? ",net" : "") + "\t" + m.ReturnType.FullName.Replace(" ", ""));
            }

            // ---- fields
            foreach (var f in td.Fields)
            {
                if (!Visible(f)) continue;
                if (f.IsSpecialName || f.Name == "value__") continue;
                if (IsCompilerGenerated(f, f.Name)) continue;
                memberCount++;
                string key = td.FullName + "::" + f.Name;
                string reach;
                if (!live.Contains(key)) reach = "dead";
                else if (externalRef.Contains(key)) reach = "used-shipped";
                else if (selfRef.ContainsKey(key)) reach = "used-self";
                else if (ReflectionMention(td.FullName, f.Name)) reach = "refl";
                else reach = "live";
                var fflags = new List<string>();
                if (f.IsStatic) fflags.Add("static");
                if (f.IsLiteral) fflags.Add("const");
                if (f.IsInitOnly) fflags.Add("readonly");
                if (fflags.Count == 0) fflags.Add("-");
                bool fui = MemberIsUi(f);
                lines.Add("F\t" + td.FullName + "\t" + f.Name + "\t0\t" + MemberVisibility(f) + "\t"
                    + string.Join(",", fflags) + "\t" + reach + "\t-" + (fui ? ",ui" : "") + "\t" + f.FieldType.FullName.Replace(" ", ""));
            }

            // ---- properties
            foreach (var p in td.Properties)
            {
                var get = p.GetMethod; var set = p.SetMethod;
                if (get != null && !Visible(get)) get = null;
                if (set != null && !Visible(set)) set = null;
                if (get == null && set == null) continue;
                if (IsCompilerGenerated(p, p.Name)) continue;
                memberCount++;
                string getKey = get != null ? td.FullName + "::" + get.Name + "(" + get.Parameters.Count + ")" : null;
                string setKey = set != null ? td.FullName + "::" + set.Name + "(" + set.Parameters.Count + ")" : null;
                bool getLive = getKey != null && live.Contains(getKey);
                bool setLive = setKey != null && live.Contains(setKey);
                string reach;
                if (getLive || setLive)
                {
                    bool ext = (getKey != null && externalRef.Contains(getKey)) || (setKey != null && externalRef.Contains(setKey));
                    bool self = (getKey != null && selfRef.ContainsKey(getKey)) || (setKey != null && selfRef.ContainsKey(setKey));
                    reach = ext ? "used-shipped" : self ? "used-self" : "live";
                }
                else if (ReflectionMention(td.FullName, p.Name)) reach = "refl";
                else reach = "dead";
                var pflags = new List<string>();
                if ((get != null && get.IsStatic) || (set != null && set.IsStatic)) pflags.Add("static");
                if (p.GetMethod == null) pflags.Add("set-only");
                if (p.SetMethod == null) pflags.Add("get-only");
                if (get != null && get.IsVirtual) pflags.Add("virtual");
                if (pflags.Count == 0) pflags.Add("-");
                bool pui = (get != null && MemberIsUi(get)) || (set != null && MemberIsUi(set));
                lines.Add("P\t" + td.FullName + "\t" + p.Name + "\t" + p.Parameters.Count + "\t"
                    + (get != null ? MemberVisibility(get) : MemberVisibility(set)) + "\t"
                    + string.Join(",", pflags) + "\t" + reach + "\t-" + (pui ? ",ui" : "") + "\t" + p.PropertyType.FullName.Replace(" ", ""));
            }

            // ---- events
            foreach (var e in td.Events)
            {
                var add = e.AddMethod; var rem = e.RemoveMethod;
                if ((add == null || !Visible(add)) && (rem == null || !Visible(rem))) continue;
                memberCount++;
                string reach;
                string addKey = add != null ? td.FullName + "::" + add.Name + "(" + add.Parameters.Count + ")" : null;
                string remKey = rem != null ? td.FullName + "::" + rem.Name + "(" + rem.Parameters.Count + ")" : null;
                bool used = (addKey != null && live.Contains(addKey)) || (remKey != null && live.Contains(remKey));
                if (used) reach = "used-shipped";
                else if (ReflectionMention(td.FullName, e.Name)) reach = "refl";
                else reach = "dead";
                lines.Add("E\t" + td.FullName + "\t" + e.Name + "\t" + e.EventType.FullName.Replace(" ", "") + "\t-\t-\t" + reach + "\t-");
            }
        }

        File.WriteAllLines(outPath, lines);
        Console.WriteLine("types=" + typeCount + " members=" + memberCount + " refs=" + referenced.Count
            + " strings=" + stringLiterals.Count + " shipped=" + string.Join(",", refAssemblies.OrderBy(x => x)));
    }
}
