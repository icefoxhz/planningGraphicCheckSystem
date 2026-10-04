package com.hz.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * @author saber
 */ //ZLib压缩工具
@Slf4j
public class ZLibUtil {

    //压缩直接数组
    public static byte[] compress(byte[] data) {
        byte[] output;

        Deflater compressor = new Deflater();

        compressor.reset();
        compressor.setInput(data);
        compressor.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[1024];
            while (!compressor.finished()) {
                int i = compressor.deflate(buf);
                bos.write(buf, 0, i);
            }
            output = bos.toByteArray();
        } catch (Exception e) {
            output = data;
            log.error(e.toString());
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                log.error(e.toString());
            }
        }
        compressor.end();
        return output;
    }

    //压缩 字节数组到输出流
    public static void compress(byte[] data, OutputStream os) {
        DeflaterOutputStream dos = new DeflaterOutputStream(os);

        try {
            dos.write(data, 0, data.length);

            dos.finish();

            dos.flush();
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

    //解压缩 字节数组
    public static byte[] decompress(byte[] data) {
        byte[] output;

        Inflater decompressor = new Inflater();
        decompressor.reset();
        decompressor.setInput(data);

        ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[1024];
            while (!decompressor.finished()) {
                int i = decompressor.inflate(buf);
                o.write(buf, 0, i);
            }
            output = o.toByteArray();
        } catch (Exception e) {
            output = data;
            log.error(e.toString());
        } finally {
            try {
                o.close();
            } catch (IOException e) {
                log.error(e.toString());
            }
        }

        decompressor.end();
        return output;
    }

    //解压缩 输入流 到字节数组
    public static byte[] decompress(InputStream is) {
        InflaterInputStream iis = new InflaterInputStream(is);
        ByteArrayOutputStream o = new ByteArrayOutputStream(1024);
        try {
            int i = 1024;
            byte[] buf = new byte[i];

            while ((i = iis.read(buf, 0, i)) > 0) {
                o.write(buf, 0, i);
            }

        } catch (IOException e) {
            log.error(e.toString());
        }
        return o.toByteArray();
    }

    public static String compressAndBase64(String json) {
        byte[] compressed = compress(json.getBytes());
        return Base64.getEncoder().encodeToString(compressed); // 转成字符串发送
    }


    public static void unzipFile(String zipFilePath, String destDir) throws IOException {
        File dir = new File(destDir);
        // 如果目标目录不存在，则创建它
        if (!dir.exists()) dir.mkdirs();

        try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(Paths.get(zipFilePath)))) {
            ZipEntry entry = zipIn.getNextEntry();
            // 循环处理ZIP文件中的每个条目
            while (entry != null) {
                String filePath = destDir + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    // 如果条目是文件，则解压
                    extractFile(zipIn, filePath);
                } else {
                    // 如果条目是目录，则创建目录
                    File newDir = new File(filePath);
                    newDir.mkdirs();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(Paths.get(filePath)));
        byte[] bytesIn = new byte[4096];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}