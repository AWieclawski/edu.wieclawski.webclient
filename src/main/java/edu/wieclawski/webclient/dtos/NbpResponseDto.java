package edu.wieclawski.webclient.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class NbpResponseDto<T> {
	private String table;
	private String currency;
	private String code;
	private T rates;

}
