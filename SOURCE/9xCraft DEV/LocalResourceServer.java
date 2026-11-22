import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

public class LocalResourceServer {

    private static final int MAX_LEVEL_SIZE = 134217728;
    private static final int MAX_REQUEST_BODY = MAX_LEVEL_SIZE + 262144;

    private final File resourcesDirectory;
    private final File savesDirectory;
    private final String skinUsername;
    private final byte[] skinBytes;
    private final ServerSocket serverSocket;
    private final int port;
    private final int preferredPort;
    private volatile boolean running;
    private Thread acceptThread;

    public LocalResourceServer(File directory, int preferredPort) throws IOException {
        this(directory, preferredPort, null, null);
    }

    public LocalResourceServer(File directory, int preferredPort, String skinUsername, byte[] skinBytes) throws IOException {
        if (directory == null) {
            throw new IOException("No resource directory was supplied.");
        }

        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create resource directory: " + directory.getAbsolutePath());
        }

        if (!directory.isDirectory()) {
            throw new IOException("Resource path is not a directory: " + directory.getAbsolutePath());
        }

        resourcesDirectory = directory.getCanonicalFile();
        this.skinUsername = skinUsername == null ? "" : skinUsername.trim();
        this.skinBytes = skinBytes == null ? null : (byte[])skinBytes.clone();
        File minecraftDirectory = resourcesDirectory.getParentFile();
        if (minecraftDirectory == null) {
            throw new IOException("Could not determine the local Minecraft directory.");
        }
        savesDirectory = new File(minecraftDirectory, "indev-worlds").getCanonicalFile();
        this.preferredPort = preferredPort;

        ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);

        if (preferredPort > 0) {
            try {
                socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), preferredPort), 50);
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException ignored) {}
                socket = new ServerSocket();
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 50);
            }
        } else {
            socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 50);
        }

        serverSocket = socket;
        port = serverSocket.getLocalPort();
        running = true;

        acceptThread = new Thread(new Runnable() {
            public void run() {
                acceptConnections();
            }
        }, "9xCraft Local Resource Server");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public int getPort() {
        return port;
    }

    public int getPreferredPort() {
        return preferredPort;
    }

    public boolean isUsingPreferredPort() {
        return preferredPort <= 0 || port == preferredPort;
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {}
    }

    private void acceptConnections() {
        while (running) {
            try {
                final Socket client = serverSocket.accept();
                Thread worker = new Thread(new Runnable() {
                    public void run() {
                        handleConnection(client);
                    }
                }, "9xCraft Resource Request");
                worker.setDaemon(true);
                worker.start();
            } catch (IOException e) {
                if (running) {
                    running = false;
                }
            }
        }
    }

    private void handleConnection(Socket socket) {
        try {
            socket.setSoTimeout(10000);
            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            String requestLine = readHttpLine(input);

            if (requestLine == null) {
                return;
            }

            String[] requestParts = requestLine.split("\\s+");
            if (requestParts.length < 2) {
                writeError(socket, 400, "Bad Request");
                return;
            }

            String method = requestParts[0].toUpperCase(Locale.ENGLISH);
            String target = requestParts[1];
            String hostHeader = null;
            List<String> headers = new ArrayList<String>();
            String headerLine;
            while ((headerLine = readHttpLine(input)) != null && headerLine.length() > 0) {
                headers.add(headerLine);
                int colon = headerLine.indexOf(':');
                if (colon > 0 && headerLine.substring(0, colon).trim().equalsIgnoreCase("Host")) {
                    hostHeader = headerLine.substring(colon + 1).trim();
                }
            }

            URL requestUrl;
            try {
                if (target.startsWith("http://") || target.startsWith("https://")) {
                    requestUrl = new URL(target);
                } else {
                    if (hostHeader == null || hostHeader.length() == 0) {
                        writeError(socket, 400, "Bad Request");
                        return;
                    }
                    requestUrl = new URL("http://" + hostHeader + target);
                }
            } catch (Exception e) {
                writeError(socket, 400, "Bad Request");
                return;
            }

            String host = requestUrl.getHost().toLowerCase(Locale.ENGLISH);
            String path;
            try {
                path = URLDecoder.decode(requestUrl.getPath().replace("+", "%2B"), "UTF-8");
            } catch (Exception e) {
                writeError(socket, 400, "Bad Request");
                return;
            }

            boolean minecraftHost = host.equals("minecraft.net") || host.endsWith(".minecraft.net");
            boolean amazonHost = host.equals("s3.amazonaws.com");
            boolean headOnly = method.equals("HEAD");
            boolean readOnlyMethod = method.equals("GET") || headOnly;
            String lowerPath = path.toLowerCase(Locale.ENGLISH);

            String requestedSkinUsername = getRequestedSkinUsername(host, lowerPath);
            if (requestedSkinUsername != null && requestedSkinUsername.equalsIgnoreCase(skinUsername) && skinBytes != null) {
                if (!readOnlyMethod) {
                    writeError(socket, 405, "Method Not Allowed");
                    return;
                }
                writeBinaryResponse(socket, 200, "OK", "image/png", skinBytes, headOnly);
                return;
            }

            if (minecraftHost && isIndevGameCheckPath(lowerPath)) {
                writeTextResponse(socket, 200, "OK", "text/plain; charset=UTF-8", "0", headOnly);
                return;
            }

            if (minecraftHost && lowerPath.endsWith("/listmaps.jsp")) {
                writeTextResponse(socket, 200, "OK", "text/plain; charset=UTF-8", listLocalWorlds(getQueryParameter(requestUrl.getQuery(), "user")), headOnly);
                return;
            }

            if (minecraftHost && lowerPath.endsWith("/level/save.html")) {
                if (!method.equals("POST")) {
                    writeError(socket, 405, "Method Not Allowed");
                    return;
                }
                handleLocalWorldSave(socket, input, headers, requestUrl);
                return;
            }

            if (minecraftHost && lowerPath.endsWith("/level/load.html")) {
                if (!readOnlyMethod) {
                    writeError(socket, 405, "Method Not Allowed");
                    return;
                }
                handleLocalWorldLoad(socket, requestUrl, headOnly);
                return;
            }

            if (minecraftHost && (path.equals("/resources") || path.startsWith("/resources/"))) {
                if (!readOnlyMethod) {
                    writeError(socket, 405, "Method Not Allowed");
                    return;
                }
                String relative = path.equals("/resources") ? "" : path.substring("/resources/".length());
                if (relative.length() == 0) {
                    writeTextIndex(socket, headOnly);
                } else {
                    serveResourceFile(socket, relative, headOnly);
                }
                return;
            }

            if (amazonHost && (path.equalsIgnoreCase("/MinecraftResources") || path.toLowerCase(Locale.ENGLISH).startsWith("/minecraftresources/"))) {
                if (!readOnlyMethod) {
                    writeError(socket, 405, "Method Not Allowed");
                    return;
                }
                String relative = path.equalsIgnoreCase("/MinecraftResources") ? "" : path.substring("/MinecraftResources/".length());
                if (relative.length() == 0) {
                    writeXmlIndex(socket, headOnly);
                } else {
                    serveResourceFile(socket, relative, headOnly);
                }
                return;
            }

            forwardRequest(socket, input, method, requestUrl, headers);
        } catch (Exception ignored) {
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private String getRequestedSkinUsername(String host, String lowerPath) {
        String relative = null;

        if ((host.equals("minecraft.net") || host.endsWith(".minecraft.net")) && lowerPath.startsWith("/skin/")) {
            relative = lowerPath.substring("/skin/".length());
        } else if (host.equals("s3.amazonaws.com") && lowerPath.startsWith("/minecraftskins/")) {
            relative = lowerPath.substring("/minecraftskins/".length());
        } else if (host.equals("skins.minecraft.net") && lowerPath.startsWith("/minecraftskins/")) {
            relative = lowerPath.substring("/minecraftskins/".length());
        }

        if (relative == null || relative.indexOf('/') >= 0 || !relative.endsWith(".png")) {
            return null;
        }

        return relative.substring(0, relative.length() - 4);
    }

    private boolean isIndevGameCheckPath(String path) {
        return path.equals("/game") || path.equals("/game/") || path.endsWith("/game/") || path.endsWith("/game");
    }

    private void handleLocalWorldSave(Socket socket, InputStream input, List<String> headers, URL requestUrl) throws IOException {
        try {
            byte[] body = readRequestBody(input, headers);
            DataInputStream data = new DataInputStream(new ByteArrayInputStream(body));

            String username = data.readUTF();
            data.readUTF();
            String levelName = data.readUTF();
            int slot = data.readUnsignedByte();
            int length = data.readInt();

            if (slot < 0 || slot > 4 || length < 0 || length > MAX_LEVEL_SIZE) {
                writeTextResponse(socket, 200, "OK", "text/plain; charset=UTF-8", "fail\n", false);
                return;
            }

            byte[] levelData = new byte[length];
            data.readFully(levelData);

            if (username == null || username.trim().length() == 0) {
                username = getQueryParameter(requestUrl.getQuery(), "user");
            }

            File userDirectory = getUserSaveDirectory(username);
            if (!userDirectory.exists() && !userDirectory.mkdirs()) {
                writeTextResponse(socket, 200, "OK", "text/plain; charset=UTF-8", "fail\n", false);
                return;
            }

            File levelFile = new File(userDirectory, "slot" + slot + ".dat");
            File nameFile = new File(userDirectory, "slot" + slot + ".name");
            File levelTemp = new File(userDirectory, "slot" + slot + ".dat.tmp");
            File nameTemp = new File(userDirectory, "slot" + slot + ".name.tmp");

            FileOutputStream levelOutput = new FileOutputStream(levelTemp);
            try {
                levelOutput.write(levelData);
            } finally {
                levelOutput.close();
            }

            String safeLevelName = cleanLevelName(levelName);
            FileOutputStream nameOutput = new FileOutputStream(nameTemp);
            try {
                nameOutput.write(safeLevelName.getBytes("UTF-8"));
            } finally {
                nameOutput.close();
            }

            replaceFile(levelTemp, levelFile);
            replaceFile(nameTemp, nameFile);
            writeTextResponse(socket, 200, "OK", "text/plain; charset=UTF-8", "ok\n", false);
        } catch (Exception e) {
            writeTextResponse(socket, 200, "OK", "text/plain; charset=UTF-8", "fail\n", false);
        }
    }

    private void handleLocalWorldLoad(Socket socket, URL requestUrl, boolean headOnly) throws IOException {
        String username = getQueryParameter(requestUrl.getQuery(), "user");
        int slot = parseSlot(getQueryParameter(requestUrl.getQuery(), "id"));

        if (slot < 0 || slot > 4) {
            writeTextResponse(socket, 200, "OK", "text/plain; charset=UTF-8", "fail\n", headOnly);
            return;
        }

        File levelFile = new File(getUserSaveDirectory(username), "slot" + slot + ".dat");
        if (!levelFile.isFile() || levelFile.length() > MAX_LEVEL_SIZE) {
            writeTextResponse(socket, 200, "OK", "text/plain; charset=UTF-8", "fail\n", headOnly);
            return;
        }

        byte[] levelData = new byte[(int)levelFile.length()];
        FileInputStream levelInput = new FileInputStream(levelFile);
        try {
            int offset = 0;
            while (offset < levelData.length) {
                int count = levelInput.read(levelData, offset, levelData.length - offset);
                if (count < 0) {
                    throw new IOException("Unexpected end of local world file.");
                }
                offset += count;
            }
        } finally {
            levelInput.close();
        }

        byte[] prefix = "ok\n".getBytes("UTF-8");
        byte[] response = new byte[prefix.length + levelData.length];
        System.arraycopy(prefix, 0, response, 0, prefix.length);
        System.arraycopy(levelData, 0, response, prefix.length, levelData.length);
        writeBinaryResponse(socket, 200, "OK", "application/octet-stream", response, headOnly);
    }

    private String listLocalWorlds(String username) {
        StringBuffer result = new StringBuffer();
        File userDirectory = getUserSaveDirectory(username);

        for (int slot = 0; slot < 5; slot++) {
            File levelFile = new File(userDirectory, "slot" + slot + ".dat");
            File nameFile = new File(userDirectory, "slot" + slot + ".name");
            String name = "-";

            if (levelFile.isFile() && nameFile.isFile()) {
                try {
                    FileInputStream input = new FileInputStream(nameFile);
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    try {
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            output.write(buffer, 0, count);
                        }
                    } finally {
                        input.close();
                    }
                    String storedName = new String(output.toByteArray(), "UTF-8");
                    if (storedName.trim().length() > 0) {
                        name = cleanLevelName(storedName);
                    }
                } catch (Exception ignored) {}
            }

            result.append(name);
            result.append(';');
        }

        return result.toString();
    }

    private File getUserSaveDirectory(String username) {
        String safeName = cleanUserName(username);
        File directory = new File(savesDirectory, safeName);
        try {
            File canonical = directory.getCanonicalFile();
            String root = savesDirectory.getCanonicalPath();
            if (!canonical.getPath().startsWith(root + File.separator)) {
                return new File(savesDirectory, "Player");
            }
            return canonical;
        } catch (IOException e) {
            return new File(savesDirectory, "Player");
        }
    }

    private String cleanUserName(String value) {
        if (value == null || value.trim().length() == 0) {
            return "Player";
        }

        StringBuffer result = new StringBuffer();
        String input = value.trim();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                result.append(c);
            }
        }
        if (result.length() == 0) {
            return "Player";
        }
        return result.toString();
    }

    private String cleanLevelName(String value) {
        if (value == null || value.length() == 0) {
            return "World";
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ';' || c == '\r' || c == '\n') {
                result.append('_');
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private int parseSlot(String value) {
        if (value == null) {
            return -1;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String getQueryParameter(String query, String name) {
        if (query == null || name == null) {
            return null;
        }
        String[] parameters = query.split("&");
        for (int i = 0; i < parameters.length; i++) {
            int equals = parameters[i].indexOf('=');
            String key = equals >= 0 ? parameters[i].substring(0, equals) : parameters[i];
            String value = equals >= 0 ? parameters[i].substring(equals + 1) : "";
            try {
                key = URLDecoder.decode(key, "UTF-8");
                value = URLDecoder.decode(value, "UTF-8");
            } catch (Exception ignored) {}
            if (key.equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    private String getHeader(List<String> headers, String name) {
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            int colon = header.indexOf(':');
            if (colon > 0 && header.substring(0, colon).trim().equalsIgnoreCase(name)) {
                return header.substring(colon + 1).trim();
            }
        }
        return null;
    }

    private byte[] readRequestBody(InputStream input, List<String> headers) throws IOException {
        String transferEncoding = getHeader(headers, "Transfer-Encoding");
        if (transferEncoding != null && transferEncoding.toLowerCase(Locale.ENGLISH).indexOf("chunked") >= 0) {
            return readChunkedBody(input);
        }

        String contentLengthText = getHeader(headers, "Content-Length");
        if (contentLengthText == null) {
            throw new IOException("Missing Content-Length header.");
        }

        int contentLength;
        try {
            contentLength = Integer.parseInt(contentLengthText);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid Content-Length header.");
        }

        if (contentLength < 0 || contentLength > MAX_REQUEST_BODY) {
            throw new IOException("World save request is too large.");
        }

        byte[] body = new byte[contentLength];
        int offset = 0;
        while (offset < contentLength) {
            int count = input.read(body, offset, contentLength - offset);
            if (count < 0) {
                throw new IOException("Unexpected end of world save request.");
            }
            offset += count;
        }
        return body;
    }

    private byte[] readChunkedBody(InputStream input) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        while (true) {
            String chunkLine = readHttpLine(input);
            if (chunkLine == null) {
                throw new IOException("Unexpected end of chunked request body.");
            }

            String sizeText = chunkLine;
            int extension = sizeText.indexOf(';');
            if (extension >= 0) {
                sizeText = sizeText.substring(0, extension);
            }

            int chunkSize;
            try {
                chunkSize = Integer.parseInt(sizeText.trim(), 16);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid chunk size.");
            }

            if (chunkSize == 0) {
                while (true) {
                    String trailer = readHttpLine(input);
                    if (trailer == null || trailer.length() == 0) {
                        break;
                    }
                }
                return body.toByteArray();
            }

            if (chunkSize < 0 || body.size() + chunkSize > MAX_REQUEST_BODY) {
                throw new IOException("Chunked world save request is too large.");
            }

            byte[] buffer = new byte[Math.min(8192, chunkSize)];
            int remaining = chunkSize;
            while (remaining > 0) {
                int count = input.read(buffer, 0, Math.min(buffer.length, remaining));
                if (count < 0) {
                    throw new IOException("Unexpected end of chunked request body.");
                }
                body.write(buffer, 0, count);
                remaining -= count;
            }

            int first = input.read();
            int second = input.read();
            if (first != '\r' || second != '\n') {
                throw new IOException("Invalid chunk terminator.");
            }
        }
    }

    private void replaceFile(File temporary, File destination) throws IOException {
        if (destination.exists() && !destination.delete()) {
            throw new IOException("Could not replace local world file.");
        }
        if (!temporary.renameTo(destination)) {
            throw new IOException("Could not finish writing local world file.");
        }
    }

    private void writeBinaryResponse(Socket socket, int status, String reason, String contentType, byte[] data, boolean headOnly) throws IOException {
        OutputStream out = socket.getOutputStream();
        writeHeaders(out, status, reason, contentType, data.length);
        if (!headOnly) {
            out.write(data);
        }
        out.flush();
    }

    private String readHttpLine(InputStream input) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int value;
        while ((value = input.read()) != -1) {
            if (value == '\n') {
                break;
            }
            if (value != '\r') {
                line.write(value);
            }
        }
        if (value == -1 && line.size() == 0) {
            return null;
        }
        return new String(line.toByteArray(), "ISO-8859-1");
    }

    private void forwardRequest(Socket client, InputStream clientInput, String method, URL requestUrl, List<String> headers) throws IOException {
        if (!requestUrl.getProtocol().equalsIgnoreCase("http")) {
            writeError(client, 502, "Bad Gateway");
            return;
        }

        int remotePort = requestUrl.getPort() > 0 ? requestUrl.getPort() : 80;
        Socket remote = new Socket();
        try {
            remote.connect(new InetSocketAddress(requestUrl.getHost(), remotePort), 10000);
            remote.setSoTimeout(15000);

            OutputStream remoteOutput = remote.getOutputStream();
            String requestTarget = requestUrl.getFile();
            if (requestTarget == null || requestTarget.length() == 0) {
                requestTarget = "/";
            }

            remoteOutput.write((method + " " + requestTarget + " HTTP/1.0\r\n").getBytes("ISO-8859-1"));

            boolean hasHost = false;
            int contentLength = 0;
            boolean hasChunkedBody = false;

            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);
                int colon = header.indexOf(':');
                String key = colon > 0 ? header.substring(0, colon).trim() : "";
                String value = colon > 0 ? header.substring(colon + 1).trim() : "";

                if (key.equalsIgnoreCase("Host")) {
                    hasHost = true;
                }
                if (key.equalsIgnoreCase("Content-Length")) {
                    try {
                        contentLength = Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {}
                }
                if (key.equalsIgnoreCase("Transfer-Encoding") && value.toLowerCase(Locale.ENGLISH).indexOf("chunked") >= 0) {
                    hasChunkedBody = true;
                }

                if (key.equalsIgnoreCase("Connection") || key.equalsIgnoreCase("Proxy-Connection") || key.equalsIgnoreCase("Proxy-Authorization")) {
                    continue;
                }

                remoteOutput.write((header + "\r\n").getBytes("ISO-8859-1"));
            }

            if (!hasHost) {
                remoteOutput.write(("Host: " + requestUrl.getHost() + "\r\n").getBytes("ISO-8859-1"));
            }
            remoteOutput.write("Connection: close\r\n\r\n".getBytes("ISO-8859-1"));

            if (hasChunkedBody) {
                copyChunked(clientInput, remoteOutput);
            } else if (contentLength > 0) {
                copyExactly(clientInput, remoteOutput, contentLength);
            }
            remoteOutput.flush();

            InputStream remoteInput = remote.getInputStream();
            OutputStream clientOutput = client.getOutputStream();
            copyUntilEnd(remoteInput, clientOutput);
            clientOutput.flush();
        } catch (IOException e) {
            try {
                writeError(client, 502, "Bad Gateway");
            } catch (IOException ignored) {}
        } finally {
            try {
                remote.close();
            } catch (IOException ignored) {}
        }
    }

    private void copyExactly(InputStream input, OutputStream output, int length) throws IOException {
        byte[] buffer = new byte[8192];
        int remaining = length;
        while (remaining > 0) {
            int count = input.read(buffer, 0, Math.min(buffer.length, remaining));
            if (count == -1) {
                throw new IOException("Unexpected end of HTTP request body.");
            }
            output.write(buffer, 0, count);
            remaining -= count;
        }
    }

    private void copyChunked(InputStream input, OutputStream output) throws IOException {
        while (true) {
            String chunkLine = readHttpLine(input);
            if (chunkLine == null) {
                throw new IOException("Unexpected end of chunked HTTP request body.");
            }
            output.write((chunkLine + "\r\n").getBytes("ISO-8859-1"));

            String sizeText = chunkLine;
            int extension = sizeText.indexOf(';');
            if (extension >= 0) {
                sizeText = sizeText.substring(0, extension);
            }

            int chunkSize;
            try {
                chunkSize = Integer.parseInt(sizeText.trim(), 16);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid chunk size.");
            }

            if (chunkSize == 0) {
                String trailer;
                while ((trailer = readHttpLine(input)) != null) {
                    output.write((trailer + "\r\n").getBytes("ISO-8859-1"));
                    if (trailer.length() == 0) {
                        break;
                    }
                }
                return;
            }

            copyExactly(input, output, chunkSize);
            int first = input.read();
            int second = input.read();
            if (first != '\r' || second != '\n') {
                throw new IOException("Invalid chunk terminator.");
            }
            output.write("\r\n".getBytes("ISO-8859-1"));
        }
    }

    private void copyUntilEnd(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
    }

    private void writeTextIndex(Socket socket, boolean headOnly) throws IOException {
        List<ResourceEntry> files = collectEntries();
        StringBuffer body = new StringBuffer();

        for (int i = 0; i < files.size(); i++) {
            ResourceEntry entry = files.get(i);
            if (entry.file == null || entry.key.indexOf('/') < 0) {
                continue;
            }
            body.append(entry.key);
            body.append(',');
            body.append(entry.file.length());
            body.append(',');
            body.append(entry.file.lastModified());
            body.append('\n');
        }

        writeTextResponse(socket, 200, "OK", "text/plain; charset=UTF-8", body.toString(), headOnly);
    }

    private void writeXmlIndex(Socket socket, boolean headOnly) throws IOException {
        List<ResourceEntry> entries = collectEntries();
        StringBuffer body = new StringBuffer();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        body.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        body.append("<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">");
        body.append("<Name>MinecraftResources</Name>");
        body.append("<Prefix></Prefix>");
        body.append("<Marker></Marker>");
        body.append("<MaxKeys>1000</MaxKeys>");
        body.append("<IsTruncated>false</IsTruncated>");

        for (int i = 0; i < entries.size(); i++) {
            ResourceEntry entry = entries.get(i);
            body.append("<Contents>");
            body.append("<Key>").append(xmlEscape(entry.key)).append("</Key>");
            body.append("<LastModified>");
            body.append(dateFormat.format(new java.util.Date(entry.file == null ? 0L : entry.file.lastModified())));
            body.append("</LastModified>");
            body.append("<Size>");
            body.append(entry.file == null ? 0L : entry.file.length());
            body.append("</Size>");
            body.append("<StorageClass>STANDARD</StorageClass>");
            body.append("</Contents>");
        }

        body.append("</ListBucketResult>");
        writeTextResponse(socket, 200, "OK", "application/xml; charset=UTF-8", body.toString(), headOnly);
    }

    private List<ResourceEntry> collectEntries() {
        TreeMap<String, ResourceEntry> entries = new TreeMap<String, ResourceEntry>();
        collectDirectory(resourcesDirectory, "", entries);
        return new ArrayList<ResourceEntry>(entries.values());
    }

    private void collectDirectory(File directory, String prefix, Map<String, ResourceEntry> entries) {
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }

        Arrays.sort(children, new Comparator<File>() {
            public int compare(File first, File second) {
                return first.getName().compareToIgnoreCase(second.getName());
            }
        });

        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            try {
                String canonical = child.getCanonicalPath();
                String rootPath = resourcesDirectory.getCanonicalPath();
                if (!canonical.equals(rootPath) && !canonical.startsWith(rootPath + File.separator)) {
                    continue;
                }

                String key = prefix + child.getName();
                if (child.isDirectory()) {
                    entries.put(key + "/", new ResourceEntry(key + "/", null));
                    collectDirectory(child, key + "/", entries);
                } else if (prefix.length() > 0 && child.isFile()) {
                    entries.put(key, new ResourceEntry(key, child));
                }
            } catch (IOException ignored) {}
        }
    }

    private void serveResourceFile(Socket socket, String relativePath, boolean headOnly) throws IOException {
        if (relativePath == null || relativePath.length() == 0) {
            writeError(socket, 404, "Not Found");
            return;
        }

        File file;
        try {
            file = new File(resourcesDirectory, relativePath.replace('/', File.separatorChar)).getCanonicalFile();
            String rootPath = resourcesDirectory.getCanonicalPath();
            if (!file.getPath().startsWith(rootPath + File.separator) || !file.isFile()) {
                writeError(socket, 404, "Not Found");
                return;
            }
        } catch (IOException e) {
            writeError(socket, 404, "Not Found");
            return;
        }

        OutputStream out = socket.getOutputStream();
        writeHeaders(out, 200, "OK", "application/octet-stream", file.length());

        if (!headOnly) {
            FileInputStream fileInput = new FileInputStream(file);
            try {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = fileInput.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
            } finally {
                fileInput.close();
            }
        }
        out.flush();
    }

    private void writeTextResponse(Socket socket, int status, String reason, String contentType, String body, boolean headOnly) throws IOException {
        byte[] data = body.getBytes("UTF-8");
        writeBinaryResponse(socket, status, reason, contentType, data, headOnly);
    }

    private void writeError(Socket socket, int status, String reason) throws IOException {
        writeTextResponse(socket, status, reason, "text/plain; charset=UTF-8", reason + "\n", false);
    }

    private void writeHeaders(OutputStream out, int status, String reason, String contentType, long contentLength) throws IOException {
        String headers = "HTTP/1.0 " + status + " " + reason + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + contentLength + "\r\n" +
                "Connection: close\r\n\r\n";
        out.write(headers.getBytes("ISO-8859-1"));
    }

    private String xmlEscape(String value) {
        String escaped = value.replace("&", "&amp;");
        escaped = escaped.replace("<", "&lt;");
        escaped = escaped.replace(">", "&gt;");
        escaped = escaped.replace("\"", "&quot;");
        escaped = escaped.replace("'", "&apos;");
        return escaped;
    }

    private static class ResourceEntry {
        private final String key;
        private final File file;

        private ResourceEntry(String key, File file) {
            this.key = key;
            this.file = file;
        }
    }
}
