package com.bitc502.grapemarket.model;

import java.sql.Timestamp;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@Entity 
public class Message {

	@Id 
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id; // 시퀀스
	private String message; //메시지 내용
	
	
	
	@ManyToOne(cascade = CascadeType.MERGE)
	@JoinColumn(name = "senderId")
	@JsonIgnoreProperties({ "comment","board","like" })
	private User senderId; 
	
	
	@ManyToOne
	@JoinColumn(name="chatId")
	private Chat chat; //id
	
	@CreationTimestamp
	private Timestamp createDate;
	@CreationTimestamp 
	private Timestamp updateDate;
	
	private String sender; 
	
	@Transient
	private int temp;
	
	@Transient
	private int temp2;
}
