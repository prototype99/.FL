package com.mmu;

import processing.core.PApplet;

public class Target {
    float x, y, s;
    Target(PApplet sketch){
        s = sketch.random(120.3F);
        x = sketch.random(sketch.width);
        y = sketch.random(sketch.height);
    }
}
