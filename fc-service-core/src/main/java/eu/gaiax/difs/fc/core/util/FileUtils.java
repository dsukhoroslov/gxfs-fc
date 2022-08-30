package eu.gaiax.difs.fc.core.util;

import eu.gaiax.difs.fc.core.pojo.ContentAccessorFile;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class FileUtils {
    public static ContentAccessorFile getAccessorByPath(String path) throws UnsupportedEncodingException {
        URL url = FileUtils.class.getClassLoader().getResource(path);
        String str = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8.name());
        File file = new File(str);
        ContentAccessorFile accessor = new ContentAccessorFile(file);
        return accessor;
    }
}
