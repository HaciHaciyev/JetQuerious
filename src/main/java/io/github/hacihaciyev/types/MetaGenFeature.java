package io.github.hacihaciyev.types;

import org.graalvm.nativeimage.hosted.Feature;

public class MetaGenFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        MetaGen.main();
    }
}
