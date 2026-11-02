package rawr.data;

import java.io.IOException;

/**
 * Fetches the raw item payload for an item ID (DECISIONS #31). Implementations never retry and
 * never call anything but the configured source; tests use local fakes (a {@code FileItemSource}
 * or a loopback HTTP server).
 */
public interface ItemSourceProvider {

    /**
     * Returns the item payload (Wowhead/AoWoW item XML) for the given item ID.
     *
     * @throws IOException when the source is unreachable, rejects the request or has no payload
     */
    String fetchXml(int itemId) throws IOException;
}
