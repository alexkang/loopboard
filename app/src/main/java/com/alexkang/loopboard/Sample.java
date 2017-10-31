package com.alexkang.loopboard;

abstract class Sample {

    abstract String getName();

    abstract void play(boolean isLooped);

    abstract void stop();

    abstract boolean isLooping();

    abstract void shutdown();
}
