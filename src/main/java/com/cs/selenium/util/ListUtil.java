package com.cs.selenium.util;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author: CS
 * @Date: 2020/9/23 10:57
 * @Description:
 */
public class ListUtil {


    /**
     * 将一个List均分成n个list,主要通过偏移量来实现的
     *
     * @param source 源集合
     * @param limit  最大值
     * @return
     */
    public static <T> List<List<T>> averageAssign(List<T> source, int limit) {
        if (null == source || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<T>> result = new ArrayList<>();
        int listCount = (source.size() - 1) / limit + 1;
        // (先计算出余数)
        int remaider = source.size() % listCount;
        // 然后是商
        int number = source.size() / listCount;
        // 偏移量
        int offset = 0;
        for (int i = 0; i < listCount; i++) {
            List<T> value;
            if (remaider > 0) {
                value = source.subList(i * number + offset, (i + 1) * number + offset + 1);
                remaider--;
                offset++;
            } else {
                value = source.subList(i * number + offset, (i + 1) * number + offset);
            }
            result.add(value);
        }
        return result;
    }

    public static void main(String[] args) {
        List list = new ArrayList();
        for (int i = 0; i < 400; i++) {
            list.add(i);
        }
        List<List> result = averageAssign(list, 15);
        result.forEach(l -> {
            l.forEach(i ->
                    System.out.print(i + "\t")
            );
            System.out.println();
        });
        System.out.println("====================================================");
        result = averageAssign(list, 20);
        result.forEach(l -> {
            l.forEach(i ->
                    System.out.print(i + "\t")
            );
            System.out.println();
        });

        System.out.println("====================================================");
        // Guava 实现不平均分组
        result = Lists.partition(list, 10);
        result.forEach(l -> {
            l.forEach(i ->
                    System.out.print(i + "\t")
            );
            System.out.println();
        });
    }
}