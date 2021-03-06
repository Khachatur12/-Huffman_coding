package com.huff;

import java.io.*;
import java.util.*;

/**
 * @author Khachatur Akopyan
 * @version 1.0
 */

public class Huffman {
    public static void main(String[] args) throws FileNotFoundException {
        HuffmanEncoder huffmanEncoder = new HuffmanEncoder();
        HuffmanDecoder huffmanDecoder = new HuffmanDecoder();
        huffmanEncoder.encode(new File(encodingFileUrl), 10000);
        huffmanDecoder.decode(new File(decodingFileDir), 800000);

    }

    static String encodingFileUrl = "C:\\Users\\xachh\\IdeaProjects\\Huffman\\src\\files\\test.txt";
    static String decodingFileDir = "C:\\Users\\xachh\\IdeaProjects\\Huffman\\src\\files\\Huffman";
}


class HuffmanEncoder {

    /**
     * Method gets a text file and encodes using the huffman method.
     */
    public void encode(File file) throws FileNotFoundException {
        HuffmanTreeCreator huffmanTreeCreator = new HuffmanTreeCreator();
        this.file = file;
        this.codesMap = huffmanTreeCreator.createCodesMap(huffmanTreeCreator.createHuffmanTree(file));
        this.charsQueue = new BlockingCharQueue(charQueueMaxLength);
        this.boolQueue = new BlockingBoolQueue(boolQueueMaxLength);

        fileRead = false;
        boolRead = false;
        charsQueue.setStopThread(false);
        boolQueue.setStopThread(false);


        CharReader charWriter = new CharReader();
        BoolReader bollWriter = new BoolReader();
        BitWriter bitWriter = new BitWriter();
        charWriter.start();
        bollWriter.start();
        bitWriter.start();

//        try {
//            Thread.sleep(4000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        System.out.println("Char writer is alive: " + charWriter.isAlive());
//        System.out.println("Bool writer is alive: " + bollWriter.isAlive());
//        System.out.println("Bit writer is alive: " + bitWriter.isAlive());
//        System.out.println(codesMap);

        try {
            charWriter.join();
            bollWriter.join();
            bitWriter.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    public void encode(File file, int MB) throws FileNotFoundException {
        this.charQueueMaxLength = MB;
        encode(file);
    }


    private class CharReader extends Thread {
        public CharReader() {
            setName("CharWriter");
        }

        @Override
        public void run() {
            try (FileReader fr = new FileReader(file)) {
                int content;
                while ((content = fr.read()) != -1) {
                    charsQueue.add((char) content);
                }

                fileRead = true;
                charsQueue.setStopThread(true);
                charsQueue.callNotify();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private class BoolReader extends Thread {
        public BoolReader() {
            setName("BoolWriter");
        }

        @Override
        public void run() {
            while (charsQueue.size() > 0 || !fileRead) {
                Character c = charsQueue.get();

                if (isInterrupted())
                    break;

                if (!codesMap.containsKey(c))
                    throw new RuntimeException("Key not found");

                for (char b : codesMap.get(c).toCharArray()) {
                    boolQueue.add(b == '1');
                }
            }

            boolQueue.setStopThread(true);
            boolRead = true;
            boolQueue.callNotify();
        }
    }


    private class BitWriter extends Thread {
        public BitWriter() {
            setName("BitWriter");
        }

        @Override
        public void run() {
            //            create new .txt file for bits
            File huffmanFolder = newDirectoryEncodedFiles(file, encodedFilesDirectoryName);
            File huffmanTextFile = new File(huffmanFolder.getPath() + "\\text.huff");
            File huffmanTreeFile = new File(huffmanFolder.getPath() + "\\tree.huff");


            try (FileOutputStream textCodeOS = new FileOutputStream(huffmanTextFile);
                 FileOutputStream treeCodeOS = new FileOutputStream(huffmanTreeFile)) {


//                Writing text bits to file text.huff
                StringBuilder byteStr;
                while (boolQueue.size() > 0 | !boolRead) {
                    while (boolQueue.size() > 8) {
                        byteStr = new StringBuilder();

                        for (int i = 0; i < 8; i++) {
                            String s = boolQueue.get() ? "1" : "0";
                            byteStr.append(s);
                        }

                        if (isInterrupted())
                            break;

                        byte b = parser.parseStringToByte(byteStr.toString());
                        textCodeOS.write(b);
                    }

                    if (fileRead && boolQueue.size() > 0) {
                        lastBitsCount = boolQueue.size();
                        byteStr = new StringBuilder();
                        for (int i = 0; i < lastBitsCount; i++) {
                            String bitStr = boolQueue.get() ? "1" : "0";
                            byteStr.append(bitStr);
                        }

                        for (int i = 0; i < 8 - lastBitsCount; i++) {
                            byteStr.append("0");
                        }

                        byte b = parser.parseStringToByte(byteStr.toString());
                        textCodeOS.write(b);
                    }
                }


//                Writing the info for decoding to tree.huff
//                Writing last bits count to tree.huff
                treeCodeOS.write((byte)lastBitsCount);

//                Writing info for Huffman tree to tree.huff
                StringBuilder treeBitsArray = new StringBuilder();
                treeBitsArray.append(
                        parser.parseByteToBinaryString(
                                (byte) codesMap.size()));

                codesMap.keySet().forEach(key -> {
                    String keyBits = parser.parseByteToBinaryString((byte) (char) key);
                    String valCountBits = parser.parseByteToBinaryString((byte) codesMap.get(key).length());

                    treeBitsArray.append(keyBits);
                    treeBitsArray.append(valCountBits);
                    treeBitsArray.append(codesMap.get(key));
                });
                if ((treeBitsArray.length() / 8) != 0) {
                    int lastBitsCount = boolQueue.size() / 8;
                    for (int i = 0; i < 7 - lastBitsCount; i++) {
                        treeBitsArray.append("0");
                    }
                }

                for (int i = 0; i < treeBitsArray.length() / 8; i++) {
                    treeCodeOS.write(parser.parseStringToByte(
                            treeBitsArray.substring(8 * i, 8 * (i + 1))));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        Parser parser = new Parser();
        int lastBitsCount;
    }


    private File newDirectoryEncodedFiles(File file, String name) {
        File huffmanFolder;
        int n = 0;
        do {
            String newFilePath = file.getParent() + "\\" + name + (n == 0 ? "" : n);
            n++;
            huffmanFolder = new File(newFilePath);
        } while (!huffmanFolder.mkdir());

        return huffmanFolder;
    }



    private File file;
    private final String encodedFilesDirectoryName = "Huffman";
    private Map<Character, String> codesMap;

    private int charQueueMaxLength = 1000;
    private int boolQueueMaxLength = charQueueMaxLength * 8;
    private BlockingCharQueue charsQueue;
    private BlockingBoolQueue boolQueue;

    private volatile boolean fileRead;
    private volatile boolean boolRead;
}


class HuffmanDecoder {


    public void decode(File fileDirectories) {
        HuffmanTreeCreator huffmanTreeCreator = new HuffmanTreeCreator();
        this.textFile = new File(fileDirectories + "\\text.huff");
        this.treeFile = new File(fileDirectories + "\\tree.huff");
        this.decodedFile = createEncodedFile(fileDirectories, "decoded");

        this.codesMap = huffmanTreeCreator.readCodesMap(treeFile);
        this.lastBitsCount = getLastBitsCount(treeFile);

        this.boolQueue = new BlockingBoolQueue(boolQueueMaxLength);
        this.charQueue = new BlockingCharQueue(charQueueMaxLength);
        this.fileRead = false;
        this.charRead = false;


        BitReader bitReader = new BitReader();
        CharReader charReader = new CharReader();
        CharWriter charWriter = new CharWriter();
        bitReader.start();
        charReader.start();
        charWriter.start();

//        try {
//            Thread.sleep(2000);
//            System.out.println("BitReader is alive: " + bitReader.isAlive());
//            System.out.println("CharReader is alive: " + charReader.isAlive());
//            System.out.println("BitWriter is alive: " + charWriter.isAlive());
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        try {
            bitReader.join();
            charReader.join();
            charWriter.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        System.out.println("Char writer is alive: " + charWriter.isAlive());
//        System.out.println("Bool writer is alive: " + bollWriter.isAlive());
//        System.out.println("Bit writer is alive: " + bitWriter.isAlive());
//        System.out.println(codesMap);
    }


    public void decode(File file, int MB) {
        this.boolQueueMaxLength = MB;
        decode(file);
    }


    private int getLastBitsCount(File treeFile) {
        try (FileInputStream fi = new FileInputStream(treeFile)) {
            return fi.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new RuntimeException("Cannot read the decoding information file");
    }


    private File createEncodedFile(File file, String name) {
        File decodedFile;
        int n = 0;
        do {
            String newFilePath = file.getParent() + "\\" + name + (n == 0 ? "" : n) + ".txt";
            n++;
            decodedFile = new File(newFilePath);
        } while (decodedFile.exists());


        return decodedFile;
    }


    private class BitReader extends Thread {
        public BitReader() {
            setName("BitReader");
        }

        @Override
        public void run() {
            try (FileInputStream fr = new FileInputStream(textFile)) {
                Parser parser = new Parser();
                int read;
                String byteStr = null;

                if ((read = fr.read()) != -1) {
                    byteStr = parser.parseByteToBinaryString((byte) read);
                }

                while ((read = fr.read()) != -1) {
                    for (int i = 0; i < 8; i++) {
                        boolQueue.add(byteStr.charAt(i) == '1');
                    }
                    byteStr = parser.parseByteToBinaryString((byte) read);
                }

                for (int i = 0; i < lastBitsCount; i++) {
                    boolQueue.add(byteStr.charAt(i) == '1');
                }

                fileRead = true;
                boolQueue.setStopThread(true);
                boolQueue.callNotify();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private class CharReader extends Thread {
        public CharReader() {
            setName("CharReader");
        }

        @Override
        public void run() {
            StringBuilder bitStr = new StringBuilder();
            Parser parser = new Parser();

            while (boolQueue.size() > 0 || !fileRead) {
                Boolean bit = boolQueue.get();

                if (isInterrupted())
                    break;

                bitStr.append(bit ? "1" : "0");

                String key = bitStr.toString();
                if (codesMap.containsKey(key)) {
                    charQueue.add(codesMap.get(key));
                    bitStr.setLength(0);
                }
            }

            charRead = true;
            charQueue.setStopThread(true);
            charQueue.callNotify();
        }
    }


    private class CharWriter extends Thread {
        public CharWriter() {
            setName("CharReader");
        }

        @Override
        public void run() {
            try (FileWriter fw = new FileWriter(decodedFile)) {
                while (charQueue.size() > 0 || !charRead) {
                    Character c = charQueue.get();

                    if (isInterrupted())
                        break;

                    fw.write(c);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private File decodedFile;
    private File textFile;
    private File treeFile;
    private Map<String, Character> codesMap;
    private int boolQueueMaxLength = 8000;
    private int charQueueMaxLength = boolQueueMaxLength / 7;
    private int lastBitsCount;
    private BlockingBoolQueue boolQueue;
    private BlockingCharQueue charQueue;
    private volatile boolean fileRead;
    private volatile boolean charRead;
}


class BlockingCharQueue {

    public BlockingCharQueue() {
    }

    public BlockingCharQueue(int maxLength) {
        this.maxLength = maxLength;
    }

    public synchronized Character get() {
        while (chars.isEmpty()) {
            if (stopThread) {
                Thread.currentThread().interrupt();
                break;
            } else {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (Thread.currentThread().isInterrupted())
            return null;

        char c = chars.poll();
        notify();
        return c;
    }

    public synchronized void add(char c) {
        while (chars.size() > maxLength) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        chars.add(c);
        notify();
    }

    public synchronized int size() {
        return chars.size();
    }

    public synchronized void callNotify() {
        this.notify();
    }

    public boolean isStopThread() {
        return stopThread;
    }

    public void setStopThread(boolean stopThread) {
        this.stopThread = stopThread;
    }


    private Queue<Character> chars = new LinkedList<>();
    private int maxLength = 10;
    private boolean stopThread;
}


class BlockingBoolQueue {

    public BlockingBoolQueue() {
    }

    public BlockingBoolQueue(int maxLength) {
        this.maxLength = maxLength;
    }

    public synchronized Boolean get() {
        while (bits.isEmpty()) {
            if (stopThread) {
                Thread.currentThread().interrupt();
                break;
            } else {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (Thread.currentThread().isInterrupted())
            return null;

        Boolean b = bits.poll();
        notify();
        return b;
    }

    public synchronized void add(boolean b) {
        while (bits.size() > maxLength) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        bits.add(b);
        notify();
    }

    public synchronized int size() {
        return bits.size();
    }

    public synchronized void callNotify() {
        notify();
    }

    public boolean isStopThread() {
        return stopThread;
    }

    public void setStopThread(boolean stopThread) {
        this.stopThread = stopThread;
    }

    private Queue<Boolean> bits = new LinkedList<>();
    private int maxLength = 90;
    private boolean stopThread;
}


class Parser {

    byte parseStringToByte(String str) {
        if (str.length() != 8)
            throw new IllegalArgumentException("String length must be 8!");

        byte b = 0;
        char[] chars = str.toCharArray();
        for (int i = 0; i < 8; i++) {
            b += (chars[i] - 48) * (1 << (7 - i));
        }

        return b;
    }

    String parseByteToBinaryString(byte b) {
        return String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }
}


class Node {
    Integer count;
    Character ch;
    Node left = null, right = null;

    public Node(Character ch, Integer count, Node left, Node right) {
        this.count = count;
        this.ch = ch;
        this.left = left;
        this.right = right;
    }
};


class HuffmanTreeCreator {
        /*
                    0
                  /   \
                 0     1
                     /   \
                    0     1
                         /  \
                        0    1
        */


    public Node createHuffmanTree(File f) throws FileNotFoundException {
        File file = f;
        FileInputStream fi = new FileInputStream(file);
        List<Node> nodes = new LinkedList<>();

//        Creating a map, where the key is characters and the values are the number of characters in the file
        Map<Character, Integer> charsMap = new LinkedHashMap<>();
        try (Reader reader = new InputStreamReader(fi);) {
            int r;
            while ((r = reader.read()) != -1) {
                char nextChar = (char) r;

                if (charsMap.containsKey(nextChar))
                    charsMap.compute(nextChar, (k, v) -> v + 1);
                else
                    charsMap.put(nextChar, 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

////        Sort the map by value and create a list
//        charsMap = charsMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

//        Creating a list of Nodes from a map
        for (Character ch : charsMap.keySet()) {
            nodes.add(new Node(ch, charsMap.get(ch), null, null));
        }

//        Creating a Huffman tree
        while (nodes.size() > 1) {
            Node firstMin = nodes.stream().min(Comparator.comparingInt(n -> n.count)).get();
            nodes.remove(firstMin);
            Node secondMin = nodes.stream().min(Comparator.comparingInt(n -> n.count)).get();
            nodes.remove(secondMin);

            Node newNode = new Node(null, firstMin.count + secondMin.count, firstMin, secondMin);
            newNode.left.count = null;
            newNode.right.count = null;
            nodes.add(newNode);
            nodes.remove(firstMin);
            nodes.remove(secondMin);
        }

//        Why first? See algorithm!
        nodes.get(0).count = null;
        return nodes.get(0);
    }


    public Map<Character, String> createCodesMap(Node treeNode) {
        Map<Character, String> codesMap = new LinkedHashMap<>();
        if (treeNode == null)
            return null;
        if (treeNode.ch != null) {
            codesMap.put(treeNode.ch, "0");
        } else {
            Node node = treeNode;
            f(node, "", codesMap);
        }

        return codesMap;
    }


    public Map<String, Character> readCodesMap(File file) {
        Map<String, Character> codesMap = null;

        try (FileInputStream fi = new FileInputStream(file)) {
//            First byte in file is last bits count
            fi.read();

            codesMap = new LinkedHashMap<>();
            StringBuilder treeBits = new StringBuilder();
            Parser parser = new Parser();

            int content;
            while ((content = fi.read()) != -1) {
                treeBits.append(parser.parseByteToBinaryString((byte) content));
            }

            int charsCount = parser.parseStringToByte(treeBits.substring(0, 8));
            treeBits.delete(0, 8);
            for (int i = 0; i < charsCount; i++) {
                char ch = (char) parser.parseStringToByte(treeBits.substring(0, 8));
                byte codeCount = parser.parseStringToByte(treeBits.substring(8, 16));
                String code = treeBits.substring(16, 16 + codeCount);
                treeBits.delete(0, 16 + codeCount);

                codesMap.put(code, ch);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return codesMap;
    }


    private void f(Node node, String code, Map<Character, String> codesMap) {
        if (node.ch != null) {
            codesMap.put(node.ch, code);
        }

        if (node.left != null)
            f(node.left, code + "0", codesMap);
        if (node.right != null)
            f(node.right, code + "1", codesMap);

    }

}
