package com.callenled.http;

/**
 * @Author: Callenld
 * @Date: 19-5-28
 */
public class Data {

    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "Data{" +
                "title='" + title + '\'' +
                '}';
    }
}
