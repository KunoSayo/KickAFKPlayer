package com.github.euonmyoji.kickafkplayer.util;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;

/**
 * @author yinyangshi
 */
public class Util {

    public static String vector3DToString(Vector3d v) {
        Vector3i i = v.toInt();
        return String.format("%s,%s,%s", i.getX(), i.getY(), i.getZ());
    }
}
