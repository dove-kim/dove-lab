package com.dove.scheduler.service;

import com.dove.jobstatus.JobStatusRegistry;
import com.dove.stockcollection.application.service.CollectionProgress;

/**
 * JobStatusRegistry를 CollectionProgress로 감싸는 어댑터.
 */
public class JobStatusProgress implements CollectionProgress {

    private final JobStatusRegistry registry;
    private final String jobName;

    public JobStatusProgress(JobStatusRegistry registry, String jobName) {
        this.registry = registry;
        this.jobName = jobName;
    }

    @Override
    public void onTotal(int total) {
        registry.start(jobName, total);
    }

    @Override
    public void onProgress(int done) {
        if (done % 100 == 0) registry.progress(jobName, done);
    }
}
