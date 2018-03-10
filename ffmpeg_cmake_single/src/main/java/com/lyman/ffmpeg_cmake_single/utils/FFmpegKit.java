package com.lyman.ffmpeg_cmake_single.utils;

import java.util.ArrayList;

/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2018/3/9
 * Description:
 */

public class FFmpegKit {
    private ArrayList<String> commands;
    static {
        System.loadLibrary("ffmpeg");
        System.loadLibrary("ffmpeginvoke");
    }


    public FFmpegKit() {
        this.commands = new ArrayList<String>();
        this.commands.add("ffmpeg");
    }

    public native static int run(String[] commands);
}
