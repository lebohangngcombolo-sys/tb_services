package com.tradebridge.identity.domain;

import java.net.InetAddress;
import java.net.UnknownHostException;

final class IpAddressParser {

    private IpAddressParser() { }

    static InetAddress parse(String value) {
        if (value == null) {
            return null;
        }
        if (!value.contains(":") && !value.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            throw new IllegalArgumentException("IP address must be an IPv4 or IPv6 literal");
        }
        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IP address", e);
        }
    }
}
