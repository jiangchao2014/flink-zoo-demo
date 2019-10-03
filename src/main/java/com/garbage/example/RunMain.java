package com.garbage.example;

import com.alibaba.tianchi.garbage_image_util.ImageClassSink;
import com.alibaba.tianchi.garbage_image_util.ImageDirSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
/**
 * Created by jiangchao08 on 2019/9/8.
 */
public class RunMain {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment flinkEnv = StreamExecutionEnvironment.getExecutionEnvironment();
        flinkEnv.setParallelism(1);
        ImageDirSource source = new ImageDirSource();
        flinkEnv.addSource(source).setParallelism(1)
                .flatMap(new PredictFlatMap()).setParallelism(4)
                .addSink(new ImageClassSink()).setParallelism(1);
        flinkEnv.execute();

    }
}
