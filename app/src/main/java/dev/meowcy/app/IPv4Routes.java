package dev.meowcy.app;

import android.net.VpnService;

import java.net.Inet4Address;

/** Adds routes covering all IPv4 addresses except one upstream proxy IP. */
final class IPv4Routes {
    static void addAllExcept(VpnService.Builder builder, Inet4Address excluded) {
        byte[] raw = excluded.getAddress();
        int ip = ((raw[0] & 0xff) << 24)
                | ((raw[1] & 0xff) << 16)
                | ((raw[2] & 0xff) << 8)
                | (raw[3] & 0xff);

        // At each level of the binary IPv4 trie, add the sibling subtree.
        // The 32 sibling prefixes together cover 0.0.0.0/0 minus excluded/32.
        int prefix = 0;
        for (int length = 1; length <= 32; length++) {
            int bitPosition = 32 - length;
            int currentBit = (ip >>> bitPosition) & 1;
            int siblingBit = currentBit ^ 1;

            int siblingPrefix = prefix | (siblingBit << bitPosition);
            builder.addRoute(toIPv4(siblingPrefix), length);

            prefix |= currentBit << bitPosition;
        }
    }

    private static String toIPv4(int value) {
        return ((value >>> 24) & 0xff) + "."
                + ((value >>> 16) & 0xff) + "."
                + ((value >>> 8) & 0xff) + "."
                + (value & 0xff);
    }

    private IPv4Routes() {}
}
