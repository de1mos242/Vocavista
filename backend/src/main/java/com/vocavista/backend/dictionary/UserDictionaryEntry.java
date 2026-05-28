package com.vocavista.backend.dictionary;

import com.vocavista.backend.auth.UserAccount;
import com.vocavista.backend.wordinfo.WordInfoRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_dictionary_entries")
@Getter
@Setter
@NoArgsConstructor
class UserDictionaryEntry {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_account_id", nullable = false)
	private UserAccount userAccount;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "word_info_record_id", nullable = false)
	private WordInfoRecord wordInfoRecord;

	@Column(name = "normalized_word", nullable = false)
	private String normalizedWord;

	@Column(name = "due_at", nullable = false)
	private OffsetDateTime dueAt;

	@Column(name = "last_reviewed_at")
	private OffsetDateTime lastReviewedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "last_result")
	private ReviewResult lastResult;

	@Column(name = "repetition_count", nullable = false)
	private int repetitionCount;

	@Column(name = "correct_streak", nullable = false)
	private int correctStreak;

	@Column(name = "lapse_count", nullable = false)
	private int lapseCount;

	@Column(name = "interval_days", nullable = false)
	private int intervalDays;

	@Column(name = "ease_factor", nullable = false)
	private double easeFactor;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	static UserDictionaryEntry create(UserAccount userAccount, WordInfoRecord wordInfoRecord, OffsetDateTime now) {
		UserDictionaryEntry entry = new UserDictionaryEntry();
		entry.id = UUID.randomUUID();
		entry.userAccount = userAccount;
		entry.wordInfoRecord = wordInfoRecord;
		entry.normalizedWord = wordInfoRecord.getNormalizedWord();
		entry.dueAt = now;
		entry.repetitionCount = 0;
		entry.correctStreak = 0;
		entry.lapseCount = 0;
		entry.intervalDays = 0;
		entry.easeFactor = 2.5;
		entry.createdAt = now;
		entry.updatedAt = now;
		return entry;
	}

	void refreshWordInfoRecord(WordInfoRecord wordInfoRecord, OffsetDateTime now) {
		this.wordInfoRecord = wordInfoRecord;
		this.updatedAt = now;
	}

}
