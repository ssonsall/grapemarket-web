package com.bitc502.grapemarket.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import com.bitc502.grapemarket.model.Board;
import com.bitc502.grapemarket.model.Comment;
import com.bitc502.grapemarket.model.Likes;
import com.bitc502.grapemarket.model.Search;
import com.bitc502.grapemarket.model.TradeState;
import com.bitc502.grapemarket.model.User;
import com.bitc502.grapemarket.repository.BoardRepository;
import com.bitc502.grapemarket.repository.CommentRepository;
import com.bitc502.grapemarket.repository.LikeRepository;
import com.bitc502.grapemarket.repository.SearchRepository;
import com.bitc502.grapemarket.repository.TradeStateRepository;
import com.bitc502.grapemarket.repository.UserRepository;
import com.bitc502.grapemarket.security.UserPrincipal;
import com.grum.geocalc.BoundingArea;
import com.grum.geocalc.Coordinate;
import com.grum.geocalc.EarthCalc;
import com.grum.geocalc.Point;

import io.sentry.Sentry;

@Service
public class BoardService {

	@Autowired
	private BoardRepository bRepo;

	@Autowired
	private UserRepository uRepo;

	@Autowired
	private SearchRepository sRepo;

	@Autowired
	private TradeStateRepository tradeStateRepo;

	@Autowired
	private CommentRepository commentRepo;

	@Autowired
	private LikeRepository likeRepo;

	@Autowired
	private TradeStateService tradeStateServ;

//	private final EntityManagerFactory entityManagerFactory;

	// 글쓰기 페이지
	public String writeForm(UserPrincipal userPrincipal, Model model) {
		try {
			Optional<User> oUser = uRepo.findById(userPrincipal.getUser().getId());
			User user = oUser.get();
			model.addAttribute("user", user);
			if (userPrincipal.getUser().getAddressAuth() == 0) {
				int authNeeded = 1;
				model.addAttribute("authNeeded", authNeeded);
				return "/user/userProfile";
			}

			return "/board/write2";
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	// 상세보기 페이지
	public Map<String, Object> detail(int id, Model model, UserPrincipal userPrincipal) {

		try {

			Map<String, Object> map = new HashMap<String, Object>();

			Optional<Board> oBoard = bRepo.findById(id);
			Board board = oBoard.get();
			map.put("board", board);

			// 댓글 불러오기
			List<Comment> comments = commentRepo.findByBoardId(board.getId());
			map.put("comments", comments);

			// 좋아요 불러오기
			Likes check = likeRepo.findByUserIdAndBoardId(userPrincipal.getUser().getId(), board.getId());
			if (check != null) {
				map.put("liked", "liked");
			}
			int likeCount = likeRepo.countByBoardId(board.getId());
			map.put("likeCount", likeCount);

			// 구매완료 누른 사용자 불러오기
			List<TradeState> tradeStates = tradeStateRepo.findByBoardIdAndState(board.getId());
			map.put("tradeStates", tradeStates);

			return map;
		} catch (Exception e) {
			e.printStackTrace();
			Sentry.capture(e);
		}
		return null;
	}

	// 글 삭제
	public String delete(int id) {
		try {
			bRepo.deleteById(id);
			return "redirect:/board/page?page=0&category=1&userInput=&range=5";
		} catch (Exception e) {
			e.printStackTrace();
			Sentry.capture(e);
		}
		return "redirect:/board/detail/" + id;
	}

	// 글 수정
	public Board updateForm(int id, Model model) {
		try {
			Optional<Board> oBoard = bRepo.findById(id);
			Board board = oBoard.get();
			return board;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Transactional
	public void boardComplete(User user, Board board) {
		try {
			Optional<Board> oBoard = bRepo.findById(board.getId());
			Board board2 = oBoard.get();
			TradeState state = tradeStateRepo.findByUserIdAndBoardId(user.getId(), board.getId());
			if (board.getBuyer() != null) {
				board2.setState("1");
				board2.setBuyer(board.getBuyer());
				if (state.getState().equals("판매중")) {
					state.setState("판매완료");
				}

			} else {
				board2.setState("-1");
				if (state.getState().equals("판매중")) {
					state.setState("판매취소");
				}
			}

			tradeStateRepo.save(state);
			bRepo.save(board2);

			List<TradeState> tradeStates = tradeStateRepo.findByBoardIdAndState(board.getId());

			if (board.getBuyer() == null) {

				for (TradeState tradeState : tradeStates) {
					tradeState.setState("구매취소");
				} // end of for

			} else {

				for (TradeState tradeState : tradeStates) {
					if (tradeState.getUser().getId() != board.getBuyer().getId()) {
						tradeState.setState("구매취소");
					} else {
						if (tradeState.getState().equals("구매중")) {
							tradeState.setState("구매완료");
						}
					}
				} // end of for
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// 검색어 저장
	public void saveKeyword(String userInput) {
		if (!userInput.equals("")) {
			String[] searchContent = userInput.split(" ");
			for (String entity : searchContent) {
				Search search = new Search();
				entity.trim();
				if (!entity.equals("")) {
					search.setContent(entity);
					sRepo.save(search);
				}
			}
		}
	}

	// 리스트
	public Page<Board> getList(String userInput, String category, UserPrincipal userPrincipal, int range,
			Pageable pageable) {
		Page<Board> boards = null;

		Coordinate lat = Coordinate.fromDegrees(userPrincipal.getUser().getAddressX());
		Coordinate lng = Coordinate.fromDegrees(userPrincipal.getUser().getAddressY());
		Point Mine = Point.at(lat, lng);
		BoundingArea area = EarthCalc.around(Mine, range * 1000);
		Point nw = area.northWest;
		Point se = area.southEast;

		if (userInput.equals("")) {
			if (category.equals("1")) {// 입력값 공백 + 카테고리 전체 (그냥 전체 리스트)
				boards = bRepo.findAllAndGps(nw.latitude, se.latitude, nw.longitude, se.longitude, pageable);
			} else {// 입력값 공백이면 + 카테고리 (입력값조건 무시 카테고리만 걸고)
				boards = bRepo.findByCategoryAndGps(nw.latitude, se.latitude, nw.longitude, se.longitude, category,
						pageable);
			}
		} else {
			// 공백제거
			userInput = userInput.trim();
			// 정규식 형태 만들어주기
			userInput = userInput.replace(" ", ")(?=.*");
			userInput = "(?=.*" + userInput + ")";
			if (category.equals("1")) // 입력값 + 카테고리 전체 (입력값만 걸고 카테고리 조건 무시)
				category = "1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16";
			boards = bRepo.findByCategoryAndGpsAndUserInput(se.latitude, nw.latitude, nw.longitude, se.longitude, category, userInput,
					pageable);
		}
		return boards;
	}
	public Page<Board> getAllList(String userInput, String category, Pageable pageable) {
		Page<Board> boards = null;

		if (userInput.equals("")) {
			if (category.equals("1")) {// 입력값 공백 + 카테고리 전체 (그냥 전체 리스트)
				boards = bRepo.findAll(pageable);
			} else {// 입력값 공백이면 + 카테고리 (입력값조건 무시 카테고리만 걸고)
				boards = bRepo.findByCategory(category,pageable);
			}
		} else {
			userInput = userInput.trim();// 공백제거
			userInput = userInput.replace(" ", ")(?=.*");// 정규식 형태 만들어주기
			userInput = "(?=.*" + userInput + ")";
			if (category.equals("1")) // 입력값 + 카테고리 전체 (입력값만 걸고 카테고리 조건 무시)
				category = "1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16";
			boards = bRepo.findByCategoryAndUserInput(category, userInput,pageable);
		}
		return boards;
	}
	
	
	public List<Board> getPopularBoard() {
		List<Board> boards = new ArrayList<Board>();
		try {
			boards = bRepo.popularBoard();
		} catch (Exception e) {
			e.printStackTrace();
			Sentry.capture(e);
		}
		return boards;
	}

	// 카운트값 받아오기
	public long getCount(long totalElements) {
		long count = 0;
		if (totalElements % 8 == 0) {
			count = totalElements / 8;
		} else {
			count = (totalElements / 8) + 1;
		}
		return count;
	}

	// 글쓰기
	public String write2(UserPrincipal userPrincipal, Board board, List<MultipartFile> productImages,
			String fileRealPath) {
		try {
			// 파일 이름 세팅 및 쓰기
			List<String> imageFileNames = new ArrayList<String>();
			int index = 0;
			for (MultipartFile multipartFile : productImages) {
				imageFileNames.add(UUID.randomUUID() + "_" + multipartFile.getOriginalFilename());

				if (multipartFile.getSize() != 0) {
					Path filePath = Paths.get(fileRealPath + imageFileNames.get(index));
					Files.write(filePath, multipartFile.getBytes());

					if (index == 0) {
						board.setImage1(imageFileNames.get(index));
					} else if (index == 1) {
						board.setImage2(imageFileNames.get(index));
					} else if (index == 2) {
						board.setImage3(imageFileNames.get(index));
					} else if (index == 3) {
						board.setImage4(imageFileNames.get(index));
					} else if (index == 4) {
						board.setImage5(imageFileNames.get(index));
					}
				}
				index++;
			}

			board.setUser(userPrincipal.getUser());
			bRepo.save(board);

			// 거래 상태 추가
			tradeStateServ.insertSellState(userPrincipal.getUser(), board);

			// 리스트 완성되면 바꿔야함
			return "redirect:/board/page?page=0&category=1&userInput=&range=5";

		} catch (Exception e) {
			e.printStackTrace();
			Sentry.capture(e);
		}
		return "redirect:/board/writeForm";
	}

	// 업데이트
	@Transactional
	public String update(UserPrincipal userPrincipal, Board boardFromController, List<MultipartFile> productImages,
			List<String> currentImages, String fileRealPath) {
		try {
			// 파일 이름 세팅 및 쓰기
			Optional<Board> oBoard = bRepo.findById(boardFromController.getId());
			Board board = oBoard.get();

			board.setTitle(boardFromController.getTitle());
			board.setContent(boardFromController.getContent());
			board.setPrice(boardFromController.getPrice());

			List<String> imageFileNames = new ArrayList<String>();
			int index = 0;
			for (MultipartFile multipartFile : productImages) {
				imageFileNames.add(UUID.randomUUID() + "_" + multipartFile.getOriginalFilename());

				if (multipartFile.getSize() != 0) {
					Path filePath = Paths.get(fileRealPath + imageFileNames.get(index));
					Files.write(filePath, multipartFile.getBytes());

					if (index == 0) {
						board.setImage1(imageFileNames.get(index));
					} else if (index == 1) {
						board.setImage2(imageFileNames.get(index));
					} else if (index == 2) {
						board.setImage3(imageFileNames.get(index));
					} else if (index == 3) {
						board.setImage4(imageFileNames.get(index));
					} else if (index == 4) {
						board.setImage5(imageFileNames.get(index));
					}
				} else {
					if (index == 0) {
						board.setImage1(currentImages.get(index));
					} else if (index == 1) {
						board.setImage2(currentImages.get(index));
					} else if (index == 2) {
						board.setImage3(currentImages.get(index));
					} else if (index == 3) {
						board.setImage4(currentImages.get(index));
					} else if (index == 4) {
						board.setImage5(currentImages.get(index));
					}
				}
				index++;
			}

			bRepo.save(board);
			Timestamp ts = sqlTimeStamp();
			updateTime(board.getId(), ts);

			// 리스트 완성되면 바꿔야함
			return "redirect:/board/page?page=0&category=1&userInput=&range=5";
		} catch (Exception e) {
			e.printStackTrace();
			Sentry.capture(e);
		}
		return "redirect:/board/writeForm";
	}

	public void updateTime(Integer id, Timestamp ts) {
		Board board = bRepo.getOne(id);
		board.setUpdateDate(ts);
	}

	public Timestamp sqlTimeStamp() {
		// java.sql.TimeStamp
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Calendar cal = Calendar.getInstance();
		String today = null;
		today = formatter.format(cal.getTime());
		Timestamp ts = Timestamp.valueOf(today);

		return ts;
	}

	// 거리값 계산후 출력될 보드데이터 불러오기
	public List<Board> getGps(UserPrincipal userPrincipal, List<Board> boardsContent, int range) {
		Coordinate lat = Coordinate.fromDegrees(userPrincipal.getUser().getAddressX());
		Coordinate lng = Coordinate.fromDegrees(userPrincipal.getUser().getAddressY());
		Point Mine = Point.at(lat, lng);
		BoundingArea area = EarthCalc.around(Mine, range * 1000);

		Point nw = area.northWest;
		Point se = area.southEast;

		List<Board> board2 = new ArrayList<Board>();
		for (int i = 0; i < boardsContent.size(); i++) {
			if (boardsContent.get(i).getUser().getAddressX() < nw.latitude
					&& boardsContent.get(i).getUser().getAddressX() > se.latitude
					&& boardsContent.get(i).getUser().getAddressY() > nw.longitude
					&& boardsContent.get(i).getUser().getAddressY() < se.longitude) {
				board2.add(boardsContent.get(i));
			}
		}
		return board2;
	}
}
