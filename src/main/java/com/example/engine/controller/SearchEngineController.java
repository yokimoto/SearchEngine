package com.example.engine.controller;

import java.util.List;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.engine.service.SearchEngineService;

/**
 * 郵便番号検索機能webエントリーポイント
 */
@Controller
public class SearchEngineController {
	
	@Autowired
	SearchEngineService searchEngineService;
	
	@RequestMapping("/")
	public String top() {
		return "top";
	}
	
	@RequestMapping("/search")
	public String search(@RequestParam String keyword, Model model) throws IOException {
		List<String> searchList = searchEngineService.searchAddress(keyword);
		int searchListSize = searchList.size();
		if(searchListSize == 0) {
			model.addAttribute("resultMessage", "1件もヒットしませんでした");			
		}
		model.addAttribute("keyword", keyword);
		model.addAttribute("searchListSize", searchListSize);
		model.addAttribute("searchList", searchList);
		return "top";
	}

	@RequestMapping("/createindex")
	public String createIndex(Model model) throws Exception {
		searchEngineService.createIndexFile();
		model.addAttribute("resultMessage", "インデックスファイルの作成が完了しました");
		return "top";
	}
	
}
