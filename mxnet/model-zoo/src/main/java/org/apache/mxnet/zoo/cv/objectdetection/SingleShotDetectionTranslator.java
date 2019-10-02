/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.apache.mxnet.zoo.cv.objectdetection;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import software.amazon.ai.Model;
import software.amazon.ai.modality.cv.BoundingBox;
import software.amazon.ai.modality.cv.DetectedObjects;
import software.amazon.ai.modality.cv.ImageTranslator;
import software.amazon.ai.modality.cv.Rectangle;
import software.amazon.ai.modality.cv.util.BufferedImageUtils;
import software.amazon.ai.ndarray.NDArray;
import software.amazon.ai.ndarray.NDList;
import software.amazon.ai.translate.TranslatorContext;
import software.amazon.ai.util.Utils;

public class SingleShotDetectionTranslator extends ImageTranslator<DetectedObjects> {

    private static final float THRESHOLD = 0.2f;

    @Override
    public NDList processInput(TranslatorContext ctx, BufferedImage input) {
        // TODO: avoid hard code image size and threshold
        input = BufferedImageUtils.resize(input, 512, 512);
        return super.processInput(ctx, input);
    }

    @Override
    public DetectedObjects processOutput(TranslatorContext ctx, NDList list) throws IOException {
        Model model = ctx.getModel();
        List<String> classes = model.getArtifact("classes.txt", Utils::readLines);

        float[] classIds = list.get(0).toFloatArray();
        float[] probabilities = list.get(1).toFloatArray();
        NDArray boundingBoxes = list.get(2);

        List<String> retNames = new ArrayList<>();
        List<Double> retProbs = new ArrayList<>();
        List<BoundingBox> retBB = new ArrayList<>();

        for (int i = 0; i < classIds.length; ++i) {
            int classId = (int) classIds[i];
            double probability = probabilities[i];
            if (classId > 0 && probability > THRESHOLD) {
                if (classId >= classes.size()) {
                    throw new AssertionError("Unexpected index: " + classId);
                }
                String className = classes.get(classId);
                float[] box = boundingBoxes.get(0, i).toFloatArray();
                double x = box[0] / 512;
                double y = box[1] / 512;
                double w = box[2] / 512 - x;
                double h = box[3] / 512 - y;

                Rectangle rect = new Rectangle(x, y, w, h);
                retNames.add(className);
                retProbs.add(probability);
                retBB.add(rect);
            }
        }

        return new DetectedObjects(retNames, retProbs, retBB);
    }
}
