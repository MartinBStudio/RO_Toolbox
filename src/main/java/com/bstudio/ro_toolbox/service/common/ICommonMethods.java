package com.bstudio.ro_toolbox.service.common;

import java.nio.file.Path;

public interface ICommonMethods {
    default String absoluteOrNull(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize().toString();
    }
}
