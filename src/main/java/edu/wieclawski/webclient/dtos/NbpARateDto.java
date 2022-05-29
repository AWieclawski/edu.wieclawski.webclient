package edu.wieclawski.webclient.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class NbpARateDto {
	private String no;
	private LocalDate effectiveDate;
	private BigDecimal mid;
}
