package com.qiushui1012.lib.qualia.ini;

/**
 * Thrown when an INI document is structurally invalid, such as an empty section
 * or a duplicated section name.
 */
public class IniException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public IniException(final String message) {
        super(message);
    }
}
