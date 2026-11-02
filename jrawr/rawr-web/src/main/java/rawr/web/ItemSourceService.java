package rawr.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rawr.base.items.Item;
import rawr.data.ItemSourceTemplates;
import rawr.data.ItemXmlParseException;
import rawr.data.ItemXmlParser;
import rawr.data.UrlTemplateItemSource;

import java.io.IOException;

/**
 * Resolves the item lookup source selected in the Item editor and fetches/parses items by ID.
 * The source is explicit (a known preset or a user-provided absolute template) and independent of
 * the icon site configured in Settings. The cookie is written by the Item editor's source control;
 * without one the wotlkdb preset is used.
 */
@Component("itemSources")
public class ItemSourceService {

    /** Cookie holding the selected item source URL template (30 days, set by the Item editor). */
    public static final String TEMPLATE_COOKIE = "jrawr_item_source_template";

    /** Read-only description of the source the Add tab will use. */
    public record SourceInfo(String template, String host, String presetKey, boolean custom) {
    }

    /** The selected valid template cookie, or an empty string when none is set. */
    public String selectedTemplate() {
        String value = cookieValue(TEMPLATE_COOKIE);
        return ItemSourceTemplates.isValidTemplate(value) ? value.trim() : "";
    }

    /** The effective URL template: the selected source, or the wotlkdb default. */
    public String resolvedTemplate() {
        String selected = selectedTemplate();
        return selected.isEmpty() ? ItemSourceTemplates.DEFAULT_TEMPLATE : selected;
    }

    /** The effective source for the current request. */
    public SourceInfo sourceInfo() {
        return sourceInfo(resolvedTemplate());
    }

    /** Description of a concrete template; invalid values fall back to the default preset. */
    public SourceInfo sourceInfo(String template) {
        String candidate = template == null ? "" : template.trim();
        String effective = ItemSourceTemplates.isValidTemplate(candidate)
                ? candidate : ItemSourceTemplates.DEFAULT_TEMPLATE;
        ItemSourceTemplates.Preset preset = ItemSourceTemplates.presetFor(effective);
        String host = ItemSourceTemplates.hostOf(effective);
        return new SourceInfo(effective, host == null ? "" : host,
                preset == null ? "custom" : preset.key(), preset == null);
    }

    /**
     * Fetches and parses one item from the effective source.
     *
     * @return the parsed item, or null when the source explicitly reports "Item not found!"
     * @throws IOException when the source cannot be reached or has no payload
     * @throws ItemXmlParseException when the payload is malformed or contains no item
     */
    public Item fetchItem(int itemId) throws IOException, ItemXmlParseException {
        String xml = new UrlTemplateItemSource(resolvedTemplate()).fetchXml(itemId);
        return ItemXmlParser.parse(xml);
    }

    private String cookieValue(String name) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
