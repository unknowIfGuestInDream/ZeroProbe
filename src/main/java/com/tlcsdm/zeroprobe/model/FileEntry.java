package com.tlcsdm.zeroprobe.model;

/**
 * Represents a file or directory entry on the remote device.
 */
public class FileEntry {

    private final String name;
    private final String path;
    private final boolean directory;
    private final long size;
    private final String permissions;
    private final String owner;
    private final String group;
    private final String lastModified;

    public FileEntry(String name, String path, boolean directory, long size,
                     String permissions, String owner, String group, String lastModified) {
        this.name = name;
        this.path = path;
        this.directory = directory;
        this.size = size;
        this.permissions = permissions;
        this.owner = owner;
        this.group = group;
        this.lastModified = lastModified;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isDirectory() {
        return directory;
    }

    public long getSize() {
        return size;
    }

    public String getPermissions() {
        return permissions;
    }

    public String getOwner() {
        return owner;
    }

    public String getGroup() {
        return group;
    }

    public String getLastModified() {
        return lastModified;
    }

    /**
     * Format file size to human-readable string.
     */
    public String getFormattedSize() {
        if (directory) {
            return "-";
        }
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
