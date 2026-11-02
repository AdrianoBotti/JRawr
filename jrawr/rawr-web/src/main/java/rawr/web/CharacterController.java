package rawr.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;
import rawr.data.ItemXmlParseException;
import rawr.data.UserItemStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Character editor (paper doll) plus the legacy import flow. The live {@link Character} lives in
 * the HTTP session; every mutation recalculates through {@link CalculationService} (serialized by
 * its global lock). Characters are persisted on demand through {@link CharacterStore} in the
 * original Rawr XML format, with unsaved-changes tracking (DECISIONS #33).
 */
@Controller
public class CharacterController {
    private final EditorService editorService;
    private final EditorViews editorViews;
    private final CharacterStore characterStore;
    private final ItemIconService itemIcons;
    private final ItemSourceService itemSources;
    private final UserItemStore userItemStore;
    private final PendingImportService pendingImports;
    private final TalentPanelService talentPanels;
    private final CalculationService calculationService;
    private final EnchantService enchantService;
    private final GemService gemService;
    private final ItemPickerService itemPicker;

    public CharacterController(
            EditorService editorService,
            EditorViews editorViews,
            CharacterStore characterStore,
            ItemIconService itemIcons,
            ItemSourceService itemSources,
            UserItemStore userItemStore,
            PendingImportService pendingImports,
            TalentPanelService talentPanels,
            CalculationService calculationService,
            EnchantService enchantService,
            GemService gemService,
            ItemPickerService itemPicker) {
        this.editorService = editorService;
        this.editorViews = editorViews;
        this.characterStore = characterStore;
        this.itemIcons = itemIcons;
        this.itemSources = itemSources;
        this.userItemStore = userItemStore;
        this.pendingImports = pendingImports;
        this.talentPanels = talentPanels;
        this.calculationService = calculationService;
        this.enchantService = enchantService;
        this.gemService = gemService;
        this.itemPicker = itemPicker;
    }

    /** Exposes the icon URL/initials helper to all templates as {@code ${icons}}. */
    @ModelAttribute("icons")
    public ItemIconService icons() {
        return itemIcons;
    }

    /** Exposes the item source helper to all templates as {@code ${itemSources}}. */
    @ModelAttribute("itemSources")
    public ItemSourceService itemSources() {
        return itemSources;
    }

    // ------------------------------------------------------------------ editor

    @GetMapping("/")
    public String editor(HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        model.addAttribute("dirty", editorService.isDirty(session));
        model.addAttribute("currentFile", editorService.currentFile(session));
        pendingImports.populate(model, session, itemSources.resolvedTemplate());
        populateEditor(model, character);
        return "index";
    }

    @GetMapping("/character")
    public String legacyCharacter() {
        return "redirect:/";
    }

    /** htmx target: the stat-details fragment the editor embeds. */
    @GetMapping("/character/ratings")
    public String ratings(HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        model.addAttribute("character", character);
        populateCalculation(model, character);
        return "fragments/stats :: statsPanel";
    }

    // ------------------------------------------------------------------ import

    /** htmx: the File > Import modal. */
    @GetMapping("/character/import")
    public String importPanel(HttpSession session, Model model) {
        pendingImports.populate(model, session, itemSources.resolvedTemplate());
        return "fragments/import :: importModal";
    }

    @GetMapping("/import")
    public String importPage() {
        return "redirect:/?open=import";
    }

    @PostMapping("/character")
    public String importCharacter(
            @RequestParam(name = "characterXml", required = false) String characterXml,
            @RequestParam(name = "file", required = false) MultipartFile file,
            HttpSession session) throws IOException {
        String xml = characterXml;
        if ((xml == null || xml.isBlank()) && file != null && !file.isEmpty()) {
            xml = new String(file.getBytes(), StandardCharsets.UTF_8);
        }
        if (xml == null || xml.isBlank()) {
            return "redirect:/?open=import&error=missing-character-xml";
        }
        Character character;
        try {
            character = editorService.loadFromXml(xml);
        } catch (Exception e) {
            return "redirect:/?open=import&error=invalid-character-xml";
        }
        String modelName = character.getCurrentModel();
        if (modelName == null || !CalculationService.MODELS.contains(modelName)) {
            return "redirect:/?open=import&error=unknown-model";
        }
        List<Integer> missing = PendingImportService.findMissingItems(character);
        if (!missing.isEmpty()) {
            // Do not silently equip unknown gear: hold the import and let the user fetch the IDs
            // from the configured item source (or import without them).
            pendingImports.store(session, xml, missing);
            return "redirect:/?open=import&pending=1";
        }
        pendingImports.clear(session);
        setImportedCharacter(session, character);
        return "redirect:/";
    }

    /**
     * Fetches the missing items of the pending import from the configured source (DECISIONS #31),
     * caches them (in memory and in the side-car store) and imports the character when nothing is
     * missing any more. Failures keep the pending import so the user can retry; the still-missing
     * IDs are recomputed from the cache, so a partial success is not repeated.
     */
    @PostMapping("/character/fetch-missing")
    public String fetchMissingItems(HttpSession session) {
        String xml = pendingImports.xml(session);
        if (xml == null) {
            return "redirect:/?open=import&error=missing-character-xml";
        }
        if (pendingImports.isFetching(session)) {
            return "redirect:/?open=import&pending=1";
        }
        pendingImports.setFetching(session, true);
        try {
            Character pending;
            try {
                pending = editorService.loadFromXml(xml);
            } catch (Exception e) {
                pendingImports.clear(session);
                return "redirect:/?open=import&error=invalid-character-xml";
            }
            List<Integer> missing = PendingImportService.findMissingItems(pending);
            if (missing.size() > PendingImportService.MAX_FETCH_ITEMS) {
                pendingImports.setError(session, "This character references " + missing.size()
                        + " missing items, more than the " + PendingImportService.MAX_FETCH_ITEMS
                        + "-item fetch limit. Add them with the Item editor in Tools instead.");
                return "redirect:/?open=import&pending=1";
            }

            int fetched = 0;
            List<String> failures = new ArrayList<>();
            for (int itemId : missing) {
                try {
                    Item item = itemSources.fetchItem(itemId);
                    if (item == null) {
                        failures.add(itemId + " (not found)");
                        continue;
                    }
                    if (item.getId() != itemId) {
                        failures.add(itemId + " (source returned item " + item.getId() + ")");
                    }
                    ItemCache.addItem(item);
                    userItemStore.add(item);
                    fetched++;
                } catch (IOException | ItemXmlParseException | RuntimeException e) {
                    failures.add(itemId + " (" + shortMessage(e) + ")");
                }
            }

            Character refreshed;
            try {
                refreshed = editorService.loadFromXml(xml);
            } catch (Exception e) {
                pendingImports.clear(session);
                return "redirect:/?open=import&error=invalid-character-xml";
            }
            List<Integer> stillMissing = PendingImportService.findMissingItems(refreshed);
            if (stillMissing.isEmpty()) {
                pendingImports.clear(session);
                setImportedCharacter(session, refreshed);
                return "redirect:/?fetched=" + fetched;
            }
            pendingImports.store(session, xml, stillMissing);
            pendingImports.setError(session, fetchFailureMessage(failures));
            return "redirect:/?open=import&pending=1";
        } finally {
            pendingImports.setFetching(session, false);
        }
    }

    /** Imports the held-back character unchanged; unknown gear stays empty until added manually. */
    @PostMapping("/character/import-pending")
    public String importPending(HttpSession session) {
        String xml = pendingImports.xml(session);
        if (xml == null) {
            return "redirect:/?open=import&error=missing-character-xml";
        }
        Character character;
        try {
            character = editorService.loadFromXml(xml);
        } catch (Exception e) {
            pendingImports.clear(session);
            return "redirect:/?open=import&error=invalid-character-xml";
        }
        String modelName = character.getCurrentModel();
        if (modelName == null || !CalculationService.MODELS.contains(modelName)) {
            pendingImports.setError(session, "The character's model is not supported yet.");
            return "redirect:/?open=import&pending=1";
        }
        pendingImports.clear(session);
        setImportedCharacter(session, character);
        return "redirect:/?imported=1";
    }

    /** Drops the held-back import (the current editor character is left untouched). */
    @PostMapping("/character/cancel-pending")
    public String cancelPending(HttpSession session) {
        pendingImports.clear(session);
        return "redirect:/?open=import";
    }

    /** Servlet multipart limit fallback: a larger upload must not surface as a 500. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String uploadTooLarge(
            @RequestHeader(name = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response,
            Model model) {
        if (hxRequest == null) {
            return "redirect:/?open=import&error=file-too-large";
        }
        return fetchError(response, model, "The uploaded file is larger than 10 MB.");
    }

    // ------------------------------------------------------------------ save / open / new / delete

    /**
     * htmx: overwrite the session's current file, or prompt for a name (Save As) when there is no
     * current file. Not-dirty characters report "No changes to save" instead of writing anything.
     */
    @PostMapping("/character/save")
    public String saveCharacter(HttpSession session, HttpServletResponse response, Model model) {
        String currentFile = editorService.currentFile(session);
        if (currentFile == null) {
            response.setHeader("HX-Retarget", "#modal");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("saveAsName",
                    characterStore.defaultName(editorService.getOrCreate(session)));
            return "fragments/file :: saveAsModal";
        }
        if (!editorService.isDirty(session)) {
            return "fragments/file :: noChangesToast";
        }
        return overwriteCurrentFile(session, model);
    }

    /** htmx: modal prompting for a Save As file name. {@code then=new} continues with a new character. */
    @GetMapping("/character/save-as")
    public String saveAsPrompt(
            @RequestParam(name = "then", required = false) String then,
            HttpSession session,
            Model model) {
        model.addAttribute("saveAsName",
                characterStore.defaultName(editorService.getOrCreate(session)));
        model.addAttribute("saveAsThen", then);
        return "fragments/file :: saveAsModal";
    }

    /**
     * htmx: write the character under an explicit name. An existing file needs a second submission
     * with {@code confirm=true}; names are never suffixed.
     */
    @PostMapping("/character/save-as")
    public String saveAs(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "confirm", defaultValue = "false") boolean confirm,
            @RequestParam(name = "then", required = false) String then,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        String fileName = CharacterStore.sanitizeSaveAsName(name);
        if (fileName == null) {
            model.addAttribute("saveAsName", CharacterStore.saveAsInputBase(name));
            model.addAttribute("saveAsThen", then);
            model.addAttribute("saveAsError", "Enter a valid file name.");
            return "fragments/file :: saveAsModal";
        }
        if (!confirm && characterStore.exists(fileName)) {
            model.addAttribute("saveAsName", CharacterStore.saveAsInputBase(name));
            model.addAttribute("saveAsThen", then);
            model.addAttribute("saveAsConfirm", true);
            model.addAttribute("saveAsFile", fileName);
            return "fragments/file :: saveAsModal";
        }
        Character character = editorService.getOrCreate(session);
        try {
            characterStore.write(fileName, character, character.getCurrentModel());
        } catch (IOException | RuntimeException e) {
            model.addAttribute("saveAsName", CharacterStore.saveAsInputBase(name));
            model.addAttribute("saveAsThen", then);
            model.addAttribute("saveAsError", "Could not save the character: " + e.getMessage());
            return "fragments/file :: saveAsModal";
        }
        editorService.setCurrentFile(session, fileName);
        editorService.clearDirty(session);
        if ("new".equals(then)) {
            editorService.startNew(session);
        }
        response.setHeader("HX-Redirect", "/?saved="
                + java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        return "fragments/file :: empty";
    }

    /**
     * Downloads the current in-memory character as original Rawr XML. The file is generated
     * through {@link Character#save(Path)} so calculation options and derived data match a normal
     * server-side save; the session character is not modified and its dirty state is preserved.
     */
    @GetMapping("/character/download")
    public ResponseEntity<byte[]> downloadCharacter(HttpSession session) {
        Character character = editorService.getOrCreate(session);
        Path temp = null;
        try {
            temp = Files.createTempFile("jrawr-download-", ".xml");
            character.save(temp);
            byte[] bytes = Files.readAllBytes(temp);
            String fileName = editorService.currentFile(session);
            if (fileName == null || fileName.isBlank()) {
                fileName = characterStore.defaultName(character) + ".xml";
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(fileName, StandardCharsets.UTF_8).build());
            headers.setContentLength(bytes.length);
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (IOException | RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not download the character: " + e.getMessage(), e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // temp file cleanup is best-effort
                }
            }
        }
    }

    /** htmx: modal listing of the saved characters with open/delete actions. */
    @GetMapping("/characters")
    public String savedCharacters(HttpSession session, Model model) {
        model.addAttribute("characters", characterStore.list());
        model.addAttribute("currentFile", editorService.currentFile(session));
        return "fragments/file :: openPanel";
    }

    /** htmx: load a saved character into the session and do a full-page redirect to the editor. */
    @PostMapping("/character/open")
    public String openCharacter(
            @RequestParam("file") String file,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        try {
            Character character = characterStore.load(file);
            editorService.openCharacter(session, character, file);
            response.setHeader("HX-Redirect", "/");
            return "fragments/file :: empty";
        } catch (Exception e) {
            model.addAttribute("openError", "Could not open " + file + ": " + e.getMessage());
            model.addAttribute("characters", characterStore.list());
            model.addAttribute("currentFile", editorService.currentFile(session));
            return "fragments/file :: openPanel";
        }
    }

    /**
     * htmx: start a new character. Dirty characters get the unsaved-changes prompt; clean ones are
     * replaced immediately and the editor reloads.
     */
    @GetMapping("/character/new")
    public String newCharacter(HttpSession session, HttpServletResponse response) {
        if (editorService.isDirty(session)) {
            return "fragments/file :: newConfirm";
        }
        editorService.startNew(session);
        response.setHeader("HX-Redirect", "/");
        return "fragments/file :: empty";
    }

    /** htmx: answer to the unsaved-changes prompt. */
    @PostMapping("/character/new")
    public String newCharacterAction(
            @RequestParam(name = "action", defaultValue = "cancel") String action,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        Character character = editorService.getOrCreate(session);
        switch (action) {
            case "save" -> {
                String currentFile = editorService.currentFile(session);
                if (currentFile == null) {
                    model.addAttribute("saveAsName", characterStore.defaultName(character));
                    model.addAttribute("saveAsThen", "new");
                    return "fragments/file :: saveAsModal";
                }
                if (editorService.isDirty(session)) {
                    try {
                        characterStore.write(currentFile, character, character.getCurrentModel());
                    } catch (IOException | RuntimeException e) {
                        model.addAttribute("errorMessage", "Could not save the character: " + e.getMessage());
                        return "fragments/file :: errorToast";
                    }
                    editorService.clearDirty(session);
                }
                editorService.startNew(session);
                response.setHeader("HX-Redirect", "/?saved="
                        + java.net.URLEncoder.encode(currentFile, StandardCharsets.UTF_8));
            }
            case "saveas" -> {
                model.addAttribute("saveAsName", characterStore.defaultName(character));
                model.addAttribute("saveAsThen", "new");
                return "fragments/file :: saveAsModal";
            }
            case "discard" -> {
                editorService.startNew(session);
                response.setHeader("HX-Redirect", "/");
            }
            case "cancel" -> {
                // Empty fragment clears the modal (hx-target="#modal" hx-swap="innerHTML").
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown action: " + action);
        }
        return "fragments/file :: empty";
    }

    /**
     * htmx: delete a saved character. Deleting the currently open file resets the editor to a fresh
     * character and reloads the page; deleting any other file just refreshes the open panel.
     */
    @PostMapping("/character/delete")
    public String deleteCharacter(
            @RequestParam("file") String file,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        try {
            characterStore.delete(file);
            if (file.equals(editorService.currentFile(session))) {
                editorService.startNew(session);
                response.setHeader("HX-Redirect", "/");
                return "fragments/file :: empty";
            }
        } catch (IOException | RuntimeException e) {
            model.addAttribute("openError", "Could not delete " + file + ": " + e.getMessage());
        }
        model.addAttribute("characters", characterStore.list());
        model.addAttribute("currentFile", editorService.currentFile(session));
        return "fragments/file :: openPanelBody";
    }

    // ------------------------------------------------------------------ item picker

    @GetMapping("/items")
    public String items(
            @RequestParam("slot") String slotName,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "relevantOnly", defaultValue = "true") boolean relevantOnly,
            @RequestParam(name = "sort", defaultValue = "overall") String sort,
            @RequestParam(name = "added", required = false) Integer added,
            HttpSession session,
            Model model) {
        CharacterSlot slot = parseSlot(slotName);
        Character character = editorService.getOrCreate(session);
        model.addAttribute("slot", slot);
        model.addAttribute("picker", itemPicker.search(character, slot, query, relevantOnly, sort, added));
        return query == null ? "fragments/item-picker :: picker"
                : "fragments/item-picker :: results";
    }

    /** htmx: enchant picker for the item currently equipped in {@code slot}. */
    @GetMapping("/enchants")
    public String enchants(
            @RequestParam("slot") String slotName,
            @RequestParam(name = "q", required = false) String query,
            HttpSession session,
            Model model) {
        CharacterSlot slot = parseSlot(slotName);
        Character character = editorService.getOrCreate(session);
        model.addAttribute("slotParam", slot.name());
        model.addAttribute("slotLabel", EditorService.slotLabel(slot));
        model.addAttribute("enchants", enchantService.choices(character, slot, query));
        return query == null ? "fragments/enchant-picker :: picker" : "fragments/enchant-picker :: results";
    }

    /** htmx: gem picker for one socket of the item currently equipped in {@code slot}. */
    @GetMapping("/gems")
    public String gems(
            @RequestParam("slot") String slotName,
            @RequestParam("socket") int socketIndex,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "sort", defaultValue = "quality") String sort,
            HttpSession session,
            Model model) {
        CharacterSlot slot = parseSlot(slotName);
        if (socketIndex < 1 || socketIndex > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown socket: " + socketIndex);
        }
        Character character = editorService.getOrCreate(session);
        ItemInstance instance = character.get(slot);
        if (instance == null || instance.getItem() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No item equipped in " + EditorService.slotLabel(slot));
        }
        model.addAttribute("slotParam", slot.name());
        model.addAttribute("socketIndex", socketIndex);
        model.addAttribute("slotLabel", EditorService.slotLabel(slot));
        model.addAttribute("socketLabel", GemService.socketLabel(character, slot, instance, socketIndex));
        model.addAttribute("sort", GemService.normalizeSort(sort));
        model.addAttribute("gems", gemService.choices(character, slot, socketIndex, query, sort));
        return query == null ? "fragments/gem-picker :: picker" : "fragments/gem-picker :: results";
    }

    @PostMapping("/character/slot")
    public String equip(
            @RequestParam("slot") String slotName,
            @RequestParam("itemId") int itemId,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        CharacterSlot slot = parseSlot(slotName);
        Item item = ItemCache.findItemById(itemId);
        if (item == null || !item.fitsInSlot(slot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Item " + itemId + " does not fit slot " + slot.name());
        }
        character.set(slot, new ItemInstance(item, null, null, null, null));
        editorService.markDirty(session);
        populateEditor(model, character);
        return "fragments/paperdoll :: paperdollSwap";
    }

    /**
     * htmx: equips one exact gemmed/enchanted instance from the comparison chart. The id is the
     * saved-file {@code itemId.gem1.gem2.gem3.enchant} form; the item must fit the slot.
     */
    @PostMapping("/character/slot/instance")
    public String equipInstance(
            @RequestParam("slot") String slotName,
            @RequestParam("gemmedId") String gemmedId,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        CharacterSlot slot = parseSlot(slotName);
        ItemInstance instance;
        try {
            instance = new ItemInstance(gemmedId);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid item instance: " + gemmedId);
        }
        if (instance.getItem() == null || !instance.getItem().fitsInSlot(slot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Item " + instance.getId() + " does not fit slot " + slot.name());
        }
        character.set(slot, instance);
        editorService.markDirty(session);
        populateEditor(model, character);
        // Keep the current screen (for example the optimizer upgrade list) and refresh the paper
        // doll and stats out of band; the chart refresh is scheduled client-side for this path.
        return "fragments/paperdoll :: paperdollOobSwap";
    }

    @PostMapping("/character/slot/enchant")
    public String enchant(
            @RequestParam("slot") String slotName,
            @RequestParam("enchantId") int enchantId,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            enchantService.apply(character, parseSlot(slotName), enchantId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        editorService.markDirty(session);
        populateEditor(model, character);
        return "fragments/paperdoll :: paperdollSwap";
    }

    @PostMapping("/character/slot/gem")
    public String gem(
            @RequestParam("slot") String slotName,
            @RequestParam("socket") int socketIndex,
            @RequestParam("gemId") int gemId,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            gemService.apply(character, parseSlot(slotName), socketIndex, gemId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        editorService.markDirty(session);
        populateEditor(model, character);
        return "fragments/paperdoll :: paperdollSwap";
    }

    @PostMapping("/character/slot/remove")
    public String remove(
            @RequestParam("slot") String slotName,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        character.set(parseSlot(slotName), null);
        editorService.markDirty(session);
        populateEditor(model, character);
        return "fragments/paperdoll :: paperdollSwap";
    }

    /**
     * htmx: character-level calculation switches from the gear summary. They are saved with the
     * character (the original paper-doll checkboxes): meta-gem enforcement and the three
     * Blacksmithing extra-socket flags.
     */
    @PostMapping("/character/calculation-options")
    public String calculationOptions(
            @RequestParam(name = "enforceGemRequirements", defaultValue = "false") boolean enforceGemRequirements,
            @RequestParam(name = "waistBlacksmithingSocket", defaultValue = "false") boolean waistSocket,
            @RequestParam(name = "wristBlacksmithingSocket", defaultValue = "false") boolean wristSocket,
            @RequestParam(name = "handsBlacksmithingSocket", defaultValue = "false") boolean handsSocket,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        character.setEnforceGemRequirements(enforceGemRequirements);
        character.setWaistBlacksmithingSocketEnabled(waistSocket);
        character.setWristBlacksmithingSocketEnabled(wristSocket);
        character.setHandsBlacksmithingSocketEnabled(handsSocket);
        character.invalidateItemInstances();
        editorService.markDirty(session);
        populateEditor(model, character);
        return "fragments/paperdoll :: paperdollSwap";
    }

    // ------------------------------------------------------------------ buffs

    @PostMapping("/character/buff")
    public String toggleBuff(
            @RequestParam("name") String name,
            @RequestParam(name = "active", defaultValue = "true") boolean active,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            editorService.toggleBuff(character, name, active);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        editorService.markDirty(session);
        populateEditor(model, character);
        return "fragments/buffs :: buffsSwap";
    }

    // ------------------------------------------------------------------ talents

    @PostMapping("/character/talents")
    public String talents(
            @RequestParam(name = "model", required = false) String modelName,
            @RequestParam(name = "talents", required = false) String talents,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        String error = null;
        try {
            editorService.applyTalents(character, modelName, talents);
            editorService.markDirty(session);
        } catch (RuntimeException e) {
            error = "Could not parse the talent string: " + e.getMessage();
        }
        populateEditor(model, character);
        model.addAttribute("talentError", error);
        return "fragments/talents :: talentsSwap";
    }

    /** htmx: learn (+1) or unlearn (-1) one talent point with the WotLK validation rules. */
    @PostMapping("/character/talents/adjust")
    public String adjustTalent(
            @RequestParam("index") int index,
            @RequestParam(name = "delta", defaultValue = "1") int delta,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        String error = null;
        try {
            talentPanels.adjustTalent(character, index, delta);
            editorService.markDirty(session);
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
        }
        populateEditor(model, character);
        model.addAttribute("talentError", error);
        return "fragments/talents :: talentsSwap";
    }

    /** htmx: toggle one modeled major/minor glyph, enforcing the three-slot limits. */
    @PostMapping("/character/glyphs/toggle")
    public String toggleGlyph(
            @RequestParam("index") int index,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        String error = null;
        try {
            talentPanels.toggleGlyph(character, index);
            editorService.markDirty(session);
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
        }
        populateEditor(model, character);
        model.addAttribute("glyphError", error);
        return "fragments/talents :: glyphsSwap";
    }

    /** htmx: clear every talent point and glyph for the current class. */
    @PostMapping("/character/talents/reset")
    public String resetTalents(HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        talentPanels.resetTalents(character);
        editorService.markDirty(session);
        populateEditor(model, character);
        return "fragments/talents :: talentsSwap";
    }

    @PostMapping("/character/glyphs/reset")
    public String resetGlyphs(HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        talentPanels.resetGlyphs(character);
        editorService.markDirty(session);
        populateEditor(model, character);
        return "fragments/talents :: glyphsSwap";
    }

    @PostMapping("/character/buffs/reset")
    public String resetBuffs(HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        editorService.resetBuffs(character);
        editorService.markDirty(session);
        populateEditor(model, character);
        return "fragments/buffs :: buffsSwap";
    }

    // ------------------------------------------------------------------ meta

    @PostMapping("/character/meta")
    public String meta(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "race", required = false) String race,
            @RequestParam(name = "model", required = false) String model,
            @RequestParam(name = "primaryProfession", required = false) String primaryProfession,
            @RequestParam(name = "secondaryProfession", required = false) String secondaryProfession,
            @RequestHeader(name = "HX-Request", required = false) String hxRequest,
            HttpSession session,
            Model viewModel) {
        Character character = editorService.getOrCreate(session);
        try {
            editorService.applyMeta(character, name, race, model);
            // The model-specific item lists (relevant items, per-slot instances) depend on the
            // loaded model, so load the just-chosen one before any template/panel touches items.
            calculationService.ensureModelLoaded(character);
            if (primaryProfession != null || secondaryProfession != null) {
                // updateProfessions runs against the globally loaded model; the model is already
                // loaded above so a model switch and a profession change stay coherent.
                editorService.applyProfessions(character, primaryProfession, secondaryProfession);
            }
            editorService.markDirty(session);
            if (hxRequest != null) {
                viewModel.addAttribute("currentFile", editorService.currentFile(session));
                populateEditor(viewModel, character);
                return "fragments/character :: characterSwap";
            }
            return "redirect:/";
        } catch (RuntimeException e) {
            return "redirect:/?error=invalid-character";
        }
    }

    // ------------------------------------------------------------------ helpers

    /** htmx: shows a toast instead of swapping the picker (used by the Add-by-ID errors). */
    private static String fetchError(HttpServletResponse response, Model model, String message) {
        response.setHeader("HX-Retarget", "#toast-area");
        response.setHeader("HX-Reswap", "innerHTML");
        model.addAttribute("errorMessage", message);
        return "fragments/file :: errorToast";
    }

    /** Short single-line message for a toast. */
    private static String shortMessage(Throwable e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        message = message.replaceAll("\\s+", " ").trim();
        return message.length() > 160 ? message.substring(0, 160) + "..." : message;
    }

    /** Makes a freshly parsed character the session character and marks it unsaved. */
    private void setImportedCharacter(HttpSession session, Character character) {
        session.setAttribute(EditorService.SESSION_CHARACTER, character);
        session.removeAttribute(EditorService.SESSION_FILE);
        session.removeAttribute(EditorService.SESSION_OPTIMIZER_SETTINGS);
        editorService.markDirty(session);
    }

    /** Condensed per-item fetch failures for the Import tab (first three, then a count). */
    private static String fetchFailureMessage(List<String> failures) {
        if (failures.isEmpty()) {
            return "Some items could not be fetched. Check the item source in Settings and retry.";
        }
        StringBuilder message = new StringBuilder("Could not fetch ");
        message.append(failures.size()).append(failures.size() == 1 ? " item: " : " items: ");
        int shown = Math.min(3, failures.size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                message.append("; ");
            }
            message.append(failures.get(i));
        }
        if (failures.size() > shown) {
            message.append("; ...");
        }
        return message.toString();
    }

    private String overwriteCurrentFile(HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        String currentFile = editorService.currentFile(session);
        try {
            characterStore.write(currentFile, character, character.getCurrentModel());
            editorService.clearDirty(session);
            model.addAttribute("savedFile", currentFile);
            return "fragments/file :: savedToast";
        } catch (IOException | RuntimeException e) {
            model.addAttribute("errorMessage", "Could not save the character: " + e.getMessage());
            return "fragments/file :: errorToast";
        }
    }

    private void populateEditor(Model model, Character character) {
        editorViews.populateEditor(model, character);
    }

    private void populateCalculation(Model model, Character character) {
        editorViews.populateCalculation(model, character);
    }

    private static CharacterSlot parseSlot(String slotName) {
        try {
            return CharacterSlot.valueOf(slotName);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown slot: " + slotName);
        }
    }

    private static String slotLabel(CharacterSlot slot) {
        String name = slot.name();
        return name.substring(0, 1).toUpperCase(Locale.ROOT)
                + name.substring(1).replaceAll("([0-9])", " $1");
    }
}
