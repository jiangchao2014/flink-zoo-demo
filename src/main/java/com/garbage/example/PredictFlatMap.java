package com.garbage.example;

import com.alibaba.tianchi.garbage_image_util.ConfigConstant;
import com.alibaba.tianchi.garbage_image_util.IdLabel;
import com.alibaba.tianchi.garbage_image_util.ImageData;
import com.intel.analytics.zoo.pipeline.inference.AbstractInferenceModel;
import com.intel.analytics.zoo.pipeline.inference.JTensor;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiangchao08 on 2019/9/16.
 */
public class PredictFlatMap implements FlatMapFunction<ImageData, IdLabel> {

    ImageClassificationModel model = new ImageClassificationModel();
    //    private String modelPath = System.getenv(ConfigConstant.IMAGE_MODEL_PATH);
    String savedModelTarPath = System.getenv(ConfigConstant.IMAGE_MODEL_PACKAGE_PATH);
    private int[] shape = { 1, 224, 224, 3 };
    private float[] meanValues = { 123.68f, 116.78f, 103.94f };

    public PredictFlatMap() throws IOException {
        //        this.model.loadTF(modelPath, 0, 0, false);
        long fileSize = new File(savedModelTarPath).length();
        InputStream inputStream = new FileInputStream(savedModelTarPath);
        byte[] modelBytes = new byte[(int) fileSize];
        inputStream.read(modelBytes);
        this.model.loadTF(modelBytes, shape, true, meanValues, 1, "input_1");
        //        this.model.loadTF();
    }

    public class ImageClassificationModel extends AbstractInferenceModel {
        public List<List<JTensor>> preProcess(byte[] imageBytes) throws IOException {
            ByteArrayInputStream in = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(in);
            // resie 224*224
            BufferedImage thumbnail = Scalr.resize(image, Scalr.Method.SPEED, Scalr.Mode.FIT_EXACT, 224, 224);

            Raster raster = thumbnail.getData();
            float[] ftmp = new float[raster.getWidth() * raster.getHeight() * raster.getNumBands()];
            System.out.println(
                    "width:" + raster.getWidth() + " height:" + raster.getHeight() + " brands:" + raster.getNumBands());
            float[] fpixels = raster.getPixels(0, 0, raster.getWidth(), raster.getHeight(), ftmp);
            System.out.println("length:" + fpixels.length);
            JTensor tensor = new JTensor(fpixels, shape);
            List<JTensor> batch = new ArrayList<JTensor>();
            batch.add(tensor);
            List<List<JTensor>> ret = new ArrayList<List<JTensor>>();
            ret.add(batch);
            return ret;
        }
    }

    public void flatMap(ImageData value, Collector<IdLabel> out) throws Exception {
        IdLabel idLabel = new IdLabel();
        idLabel.setId(value.getId());
        //        val imageMat = byteArrayToMat(bytes);
        //        val imageCent = centerCrop(imageMat, cropWidth, cropHeight);
        //        val imageTensor = matToNCHWAndRGBTensor(imageCent);
        List<List<JTensor>> inputs = model.preProcess(value.getImage());
        System.out.println("inputs:" + inputs);
        System.out.println("JT size:" + inputs.get(0).get(0).getData().length);
        List<List<JTensor>> result = model.predict(inputs);
        System.out.println("result:" + result);
        out.collect(idLabel);
    }
}
