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
package software.amazon.ai.inference;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import software.amazon.ai.Device;
import software.amazon.ai.Model;
import software.amazon.ai.metric.Metrics;
import software.amazon.ai.modality.Classification;
import software.amazon.ai.modality.cv.DetectedObjects;
import software.amazon.ai.ndarray.NDList;
import software.amazon.ai.ndarray.types.DataType;
import software.amazon.ai.ndarray.types.Shape;
import software.amazon.ai.ndarray.types.SparseFormat;
import software.amazon.ai.test.mock.EchoTranslator;
import software.amazon.ai.test.mock.MockImageTranslator;
import software.amazon.ai.test.mock.MockModel;
import software.amazon.ai.test.mock.MockNDArray;
import software.amazon.ai.translate.TranslateException;
import software.amazon.ai.translate.Translator;
import software.amazon.ai.translate.TranslatorContext;

public class InferenceTest {

    private BufferedImage image;

    @BeforeClass
    public void setup() throws IOException {
        Files.createDirectories(Paths.get("build/model"));
        image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    }

    @Test
    public void testObjectDection() throws IOException, TranslateException {
        Path modelDir = Paths.get("build/model");
        String modelName = "mockModel";

        Model model = Model.newInstance();
        model.load(modelDir, modelName);
        MockImageTranslator translator = new MockImageTranslator("cat");

        Metrics metrics = new Metrics();
        try (Predictor<BufferedImage, DetectedObjects> ssd = model.newPredictor(translator)) {
            ssd.setMetrics(metrics);
            DetectedObjects result = ssd.predict(image);

            DetectedObjects.Item object = result.items().get(0);
            Assert.assertEquals("cat", object.getClassName());
            Assert.assertEquals(
                    Double.compare(object.getBoundingBox().getBounds().getHeight(), 1d), 0);
        }
    }

    @Test
    public void testClassifier() throws IOException, TranslateException {
        Path modelDir = Paths.get("build/model");
        Model model = Model.newInstance();
        model.load(modelDir);

        final String className = "cat";
        Translator<String, Classification> translator =
                new Translator<String, Classification>() {

                    @Override
                    public NDList processInput(TranslatorContext ctx, String input) {
                        return new NDList(
                                new MockNDArray(
                                        null,
                                        null,
                                        new Shape(3, 24, 24),
                                        DataType.FLOAT32,
                                        SparseFormat.DENSE));
                    }

                    @Override
                    public Classification processOutput(TranslatorContext ctx, NDList list) {
                        return new Classification(
                                Collections.singletonList(className),
                                Collections.singletonList(0.9d));
                    }
                };
        Metrics metrics = new Metrics();

        try (Predictor<String, Classification> classifier = model.newPredictor(translator)) {
            classifier.setMetrics(metrics);
            Classification result = classifier.predict(className);
            Assert.assertEquals(Double.compare(result.get(className).getProbability(), 0.9d), 0);
        }
    }

    @Test(expectedExceptions = TranslateException.class)
    public void testTranslatException() throws TranslateException {
        EchoTranslator<String> translator = new EchoTranslator<>();
        translator.setInputException(new TranslateException("Some exception"));
        Model model = new MockModel(Device.defaultDevice());
        Predictor<String, String> predictor = model.newPredictor(translator);
        String result = predictor.predict("input");
        Assert.assertEquals(result, "input");
    }

    @Test(expectedExceptions = IOException.class)
    public void loadModelException() throws IOException {
        Path modelDir = Paths.get("build/non-exist-model");
        String modelName = "mockModel";

        try (Model model = Model.newInstance()) {
            model.load(modelDir, modelName);
        }
    }
}
