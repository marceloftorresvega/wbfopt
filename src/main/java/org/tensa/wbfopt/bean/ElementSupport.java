/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tensa.wbfopt.bean;

import java.util.List;
import lombok.Data;

/**
 *
 * @author Marcelo
 */
@Data
public class ElementSupport<T> {
    private String object;
    private String group;
    private String material;
    private String type;
    private List<T> content;
    
}
