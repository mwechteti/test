package com.example.demo;

import org.graalvm.compiler.loop.InductionVariable;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
    @Autowired
    public JobBuilderFactory jobBuilderFactory;
    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    @Lazy
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private ProfileRepository profileRepository;


    @Bean
    public RepositoryItemReader<User> reader() {
        RepositoryItemReader<User> reader = new RepositoryItemReader<>();
        reader.setRepository(userRepository);
        reader.setMethodName("findByStatusAndEmailVerified");

        List<Object> queryMethodArguments = new ArrayList<>();
        // for status
        queryMethodArguments.add("APPROVED");
        // for emailVerified
        queryMethodArguments.add(Boolean.TRUE);

        reader.setArguments(queryMethodArguments);
        reader.setPageSize(100);
        Map<String, Direction> sorts = new HashMap<>();
        sorts.put("id", Direction.ASC);
        reader.setSort(sorts);

        return reader;
    }

    @Bean
    public RepositoryItemWriter<Profile> writer() {
        RepositoryItemWriter<Profile> writer = new RepositoryItemWriter<>();
        writer.setRepository(profileRepository);
        writer.setMethodName("save");
        return writer;
    }


    @Bean
    public ProfileItemProcessor processor() {
        return new ProfileItemProcessor();
    }


    @Bean
    public Step step1(ItemReader<User> itemReader, ItemWriter<Profile> itemWriter)
            throws Exception {

        return this.stepBuilderFactory.get("step1").<User, Profile>chunk(5).reader(itemReader)
                                      .processor(processor()).writer(itemWriter).build();
    }

    @Bean
    public Job profileUpdateJob(JobCompletionNotificationListener listener, Step step1)
            throws Exception {

        return this.jobBuilderFactory.get("profileUpdateJob").incrementer(new RunIdIncrementer())
                                     .listener(listener).start(step1).build();
    }
}
