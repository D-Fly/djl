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
package software.amazon.ai.examples.inference;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.mxnet.zoo.ModelZoo;
import software.amazon.ai.Device;
import software.amazon.ai.examples.inference.util.AbstractExample;
import software.amazon.ai.examples.inference.util.Arguments;
import software.amazon.ai.inference.Predictor;
import software.amazon.ai.metric.Metrics;
import software.amazon.ai.modality.Classification;
import software.amazon.ai.modality.cv.util.BufferedImageUtils;
import software.amazon.ai.translate.TranslateException;
import software.amazon.ai.zoo.ModelNotFoundException;
import software.amazon.ai.zoo.ZooModel;

public final class ClassifyExample extends AbstractExample {

    public static void main(String[] args) {
        new ClassifyExample().runExample(args);
    }

    @Override
    public Classification predict(Arguments arguments, Metrics metrics, int iteration)
            throws IOException, ModelNotFoundException, TranslateException {
        Classification predictResult = null;
        Path imageFile = arguments.getImageFile();
        BufferedImage img = BufferedImageUtils.fromFile(imageFile);

        // Device is not not required, default device will be used by Model if not provided.
        // Change to a specific device if needed.
        Device device = Device.defaultDevice();

        Map<String, String> criteria = new ConcurrentHashMap<>();
        criteria.put("layers", "18");
        criteria.put("flavor", "v1");
        ZooModel<BufferedImage, Classification> model = ModelZoo.RESNET.loadModel(criteria, device);

        try (Predictor<BufferedImage, Classification> predictor = model.newPredictor()) {
            predictor.setMetrics(metrics); // Let predictor collect metrics

            for (int i = 0; i < iteration; ++i) {
                predictResult = predictor.predict(img);
                printProgress(iteration, i);
                collectMemoryInfo(metrics);
            }
        }

        model.close();
        return predictResult;
    }
}
