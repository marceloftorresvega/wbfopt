/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tensa.wbfopt.bean;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.Data;

/**
 *
 * @author Marcelo
 */
@Data
public class SimpleWspace {

    private Map<String, List<ElementSupport<String>>> waveNotation;
    private Map<String, List<List<MatVector>>> matFaces;
    private Map<String, String> mtlNotation;
    private BufferedImage mtlImage;

    public static SimpleWspace read(File file) throws FileNotFoundException, IOException {
        LinkedList<String> matStack = new LinkedList<>();
        LinkedList<String> objectStack = new LinkedList<>();
        LinkedList<String> groupStack = new LinkedList<>();
        final String de = "default";

        matStack.add(de);
        objectStack.add(de);
        groupStack.add(de);

        SimpleWspace sw = new SimpleWspace();
        File dirFile = file.getParentFile();

        try (FileReader fr = new FileReader(file)) {
            BufferedReader br = new BufferedReader(fr);
            Map<String, List<ElementSupport<String>>> objectReaded = br.lines()
                    .filter(s -> s.length() != 0)
                    .filter(s -> !s.startsWith("#"))
                    .map(s -> s.split(" "))
                    .peek(l -> {
                        if (l[0].equals("usemtl")) {
                            matStack.addFirst(l[1]);
                        }
                        if (l[0].equals("o")) {
                            objectStack.addFirst(l[1]);
                        }
                        if (l[0].equals("g")) {
                            groupStack.addFirst(l[1]);
                        }
                    })
                    .map(l -> {
                        ElementSupport<String> es = new ElementSupport<String>();
                        es.setObject(objectStack.getFirst());
                        es.setGroup(groupStack.getFirst());
                        es.setMaterial(matStack.getFirst());
                        es.setType(l[0]);
                        es.setContent(Arrays.asList(l));

                        return es;
                    })
                    .collect(Collectors.groupingBy(e -> e.getType()));
            sw.setWaveNotation(objectReaded);
            sw.loadMtl(dirFile);
        }
        return sw;
    }
    private File wsFile;

    private void loadMtl(File dirFile) throws FileNotFoundException, IOException {
        
        String mtllib = waveNotation.get("mtllib").stream()
                .findFirst()
                .map(ElementSupport::getContent)
                .map(List::stream)
                .map(s -> s.skip(1))
                .flatMap(s -> s.findFirst())
                .get();
        final LinkedList<String> mtlStack = new java.util.LinkedList<>();
        wsFile = dirFile;
        
        try (FileReader fr = new FileReader(new File(dirFile, mtllib))) {
            BufferedReader br = new BufferedReader(fr);
            mtlNotation = br.lines()
                    .filter(s -> s.length() != 0)
                    .filter(s -> !s.startsWith("#"))
                    .map(s -> s.split(" "))
                    .peek(sa -> {
                        if (sa[0].equals("newmtl")) {
                            mtlStack.addFirst(sa[1]);
                        }
                    })
                    .filter(sa -> sa[0].equals("map_Kd"))
                    .collect(
                            Collectors.toMap(
                                    sa -> mtlStack.getFirst(),
                                    sa -> sa[1])
                    );
        }
    }

    public void processVectors() {

        List<MatVector> vts = waveNotation.get("vt").stream()
                .map(l -> {
                    MatVector mv = new MatVector();
                    mv.setX(Double.valueOf(l.getContent().get(1)));
                    mv.setY(Double.valueOf(l.getContent().get(2)));
                    return mv;
                })
                .collect(Collectors.toList());

        matFaces = waveNotation.get("f").stream()
                .map(f -> {
                    ElementSupport<MatVector> mvl = new ElementSupport<>();
                    mvl.setMaterial(f.getMaterial());
//                            mvl.setObject(f.getObject());
//                            mvl.setGroup(f.getGroup());
//                            mvl.setType(f.getType());
                    List<MatVector> vtsx = f.getContent().stream()
                            .skip(1)
                            .map(i -> i.split("[/]"))
                            .map(l -> vts.get(Integer.valueOf(l[1]) - 1 ))
                            .collect(Collectors.toList());
                    mvl.setContent(vtsx);
                    return mvl;
                })
                .collect(Collectors.groupingBy(
                        ElementSupport::getMaterial,
                        Collectors.mapping(
                                ElementSupport::getContent,
                                Collectors.toList())));
    }

    public void readImage(String material) throws FileNotFoundException, IOException {
        mtlImage = ImageIO.read(new File(wsFile, mtlNotation.get(material)));
    }
}
