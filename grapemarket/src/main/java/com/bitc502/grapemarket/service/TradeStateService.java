package com.bitc502.grapemarket.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bitc502.grapemarket.model.Board;
import com.bitc502.grapemarket.model.TradeState;
import com.bitc502.grapemarket.model.User;
import com.bitc502.grapemarket.repository.TradeStateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TradeStateService {

	@Autowired
	private TradeStateRepository tradeStateRepo;

	public void insertSellState(User user, Board board) {

		TradeState tradeState = new TradeState();
		tradeState.setUser(user);
		tradeState.setBoard(board);
		tradeState.setState("판매중");

		tradeStateRepo.save(tradeState);
	}

	public void insertBuyState(User user, Board board) {

		int check = tradeStateRepo.countByUserAndBoard(user, board);
		if (check == 0) {

			TradeState tradeState = new TradeState();
			tradeState.setUser(user);
			tradeState.setBoard(board);
			tradeState.setState("구매중");

			tradeStateRepo.save(tradeState);
		}
	}

	public void setStateComplete(String json) {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode;
		try {
			jsonNode = mapper.readTree(json);
			JsonNode nodeUserId = jsonNode.get("userId");
			JsonNode nodeBoardId = jsonNode.get("boardId");

			int userId = Integer.parseInt(nodeUserId.toString());
			int boardId = Integer.parseInt(nodeBoardId.toString());

			TradeState ts = tradeStateRepo.findByUserIdAndBoardId(userId, boardId);

			if (ts.getState().equals("판매중")) {
				ts.setState("판매완료");
			} else if (ts.getState().equals("구매중")) {
				ts.setState("구매완료");
			}

			tradeStateRepo.save(ts);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
