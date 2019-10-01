package com.example.engine.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SearchEngineServiceTests {
	
	@Autowired
	SearchEngineService searchEngineService;

	@Test
	public void splitKeyWordTest() {
		String[] collect = {"東","京","都"};
		assertThat(searchEngineService.splitKeyword("東 京　都")).hasSize(3).containsAll(Arrays.asList(collect));
	}
	
	@Test
	public void analyseMergeTest() throws IOException {
		CSVRecord currentRecord = CSVParser.parse("1,1001000", CSVFormat.DEFAULT.withHeader("uniqueId", "zipCode")).getRecords().get(0);
		CSVRecord nextRecord = CSVParser.parse("2,1001000", CSVFormat.DEFAULT.withHeader("uniqueId", "zipCode")).getRecords().get(0);;
		String currentDetailAddress2 = "（";
		String mergeAddress = "";
		assertThat(searchEngineService.analyseMerge(currentRecord, nextRecord, currentDetailAddress2, mergeAddress)).isTrue();

		currentRecord = CSVParser.parse("1,1001000", CSVFormat.DEFAULT.withHeader("uniqueId", "zipCode")).getRecords().get(0);
		nextRecord = CSVParser.parse("2,1001001", CSVFormat.DEFAULT.withHeader("uniqueId", "zipCode")).getRecords().get(0);;
		currentDetailAddress2 = "（";
		mergeAddress = "";
		assertThat(searchEngineService.analyseMerge(currentRecord, nextRecord, currentDetailAddress2, mergeAddress)).isFalse();

		currentRecord = CSVParser.parse("1,1001000", CSVFormat.DEFAULT.withHeader("uniqueId", "zipCode")).getRecords().get(0);
		nextRecord = CSVParser.parse("2,1001000", CSVFormat.DEFAULT.withHeader("uniqueId", "zipCode")).getRecords().get(0);;
		currentDetailAddress2 = "";
		mergeAddress = "（";
		assertThat(searchEngineService.analyseMerge(currentRecord, nextRecord, currentDetailAddress2, mergeAddress)).isTrue();

		currentRecord = CSVParser.parse("1,1001000", CSVFormat.DEFAULT.withHeader("uniqueId", "zipCode")).getRecords().get(0);
		nextRecord = CSVParser.parse("2,1001000", CSVFormat.DEFAULT.withHeader("uniqueId", "zipCode")).getRecords().get(0);;
		currentDetailAddress2 = "（）";
		mergeAddress = "（";
		assertThat(searchEngineService.analyseMerge(currentRecord, nextRecord, currentDetailAddress2, mergeAddress)).isFalse();

	}

}
