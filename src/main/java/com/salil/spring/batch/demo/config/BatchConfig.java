package com.salil.spring.batch.demo.config;

import com.salil.spring.batch.demo.component.OrderFieldMapper;
import com.salil.spring.batch.demo.entity.Order;
import com.salil.spring.batch.demo.listener.CustomJobListener;
import com.salil.spring.batch.demo.processor.OrderProcessor;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import static com.salil.spring.batch.demo.constant.BatchJobConstant.*;

import javax.sql.DataSource;


@Configuration
public class BatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private OrderFieldMapper fieldMapper;

    @Value("${app.csv.fileHeaders}")
    private String[] names;

    @Bean
    public FlatFileItemReader<Order> reader() {
        return new FlatFileItemReaderBuilder<Order>()
                .name(ORDER_ITEM_READER)
                .resource(new ClassPathResource("csv/orders.csv"))
                .linesToSkip(1)
                .delimited()
                .names(names)
                .lineMapper(lineMapper())
                .fieldSetMapper(new BeanWrapperFieldSetMapper<Order>() {{
                    setTargetType(Order.class);
                }}).build();
    }

    @Bean
    public LineMapper<Order> lineMapper() {

        final DefaultLineMapper<Order> defaultLineMapper = new DefaultLineMapper<>();
        final DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setStrict(false);
        lineTokenizer.setNames(names);

        defaultLineMapper.setLineTokenizer(lineTokenizer);
        defaultLineMapper.setFieldSetMapper(fieldMapper);

        return defaultLineMapper;
    }

    @Bean
    public OrderProcessor processor() {
        return new OrderProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<Order> writer(final DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Order>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("INSERT INTO orders (order_ref, amount, order_date, note) VALUES (:orderRef, :amount, :orderDate, :note)")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public Step step(JdbcBatchItemWriter<Order> writer) {
        return stepBuilderFactory.get(BATCH_STEP)
                .<Order, Order> chunk(5)
                .reader(reader())
                .processor(processor())
                .writer(writer)
                .build();
    }

    @Bean
    public Job job(CustomJobListener listener, Step step) {
        return jobBuilderFactory.get(ORDER_PROCESS_JOB)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(step)
                .end()
                .build();
    }


}
