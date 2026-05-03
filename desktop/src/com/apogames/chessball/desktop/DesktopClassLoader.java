package com.apogames.chessball.desktop;

import com.apogames.chessball.IClassLoader;
import com.apogames.chessball.ai.ChessBallPlayerAI;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DesktopClassLoader extends ClassLoader implements IClassLoader {
    private String root;
    private String name;

    private final List<ChessBallPlayerAI> players = new ArrayList<>();

    public DesktopClassLoader() {
    }

    public ChessBallPlayerAI getAI() {
        ChessBallPlayerAI ai = null;
        Class<?> c;
        try {
            c = loadClass(this.name);
            try {
                Object o = c.getDeclaredConstructor().newInstance();
                ai = (ChessBallPlayerAI) o;
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return ai;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> c;
        try {
            c = findLoadedClass(name);

            if (c == null) {
                String filename = name.replace('.', File.separatorChar) + ".class";
                try {
                    byte[] data = loadClassData(filename);
                    c = defineClass(data, 0, data.length);
                } catch (IOException | NoClassDefFoundError e) {
                    // class file not found via our root, fall through
                }
            }

            if (c == null) {
                try {
                    c = findSystemClass(name);
                } catch (Exception ignored) {
                }
            }

            if (c == null) {
                throw new ClassNotFoundException(name);
            }
        } catch (Throwable ex) {
            throw new ClassNotFoundException(name);
        }
        return c;
    }

    private byte[] loadClassData(String filename) throws IOException {
        File f = new File(this.root, filename);
        int size = (int) f.length();
        byte[] buff = new byte[size];
        try (FileInputStream fis = new FileInputStream(f);
             DataInputStream dis = new DataInputStream(fis)) {
            dis.readFully(buff);
        }
        return buff;
    }

    @Override
    public List<ChessBallPlayerAI> loadPlayers() {
        this.players.clear();
        String filePath = DesktopClassLoader.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        File directory = new File(filePath);
        if (!directory.isDirectory()) {
            filePath = directory.getParent();
        }
        if (filePath != null) {
            this.loadPlayers(filePath);
        }
        return players;
    }

    public void loadPlayers(String filePath) {
        File directory = new File(filePath);
        if (!directory.isDirectory()) {
            return;
        }
        File[] listOfFiles = directory.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    this.root = file.getParent();
                    this.name = file.getName().substring(0, file.getName().indexOf(".class"));
                    ChessBallPlayerAI ai = getAI();
                    if (ai != null) {
                        players.add(ai);
                    }
                } else if (file.isDirectory()
                        && !file.getName().endsWith("chessball")
                        && !file.getName().contains(".")) {
                    this.loadPlayers(file.getAbsolutePath());
                }
            }
        }
    }
}
