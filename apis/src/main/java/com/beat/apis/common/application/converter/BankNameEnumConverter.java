package com.beat.apis.common.application.converter;

import com.beat.apis.performance.application.dto.BankNameType;
import com.beat.domain.performance.domain.BankName;

public final class BankNameEnumConverter {

	private BankNameEnumConverter() {
	}

	public static BankName toDomain(final BankNameType bankNameType) {
		if (bankNameType == null) {
			return null;
		}

		return switch (bankNameType) {
			case NH_NONGHYUP -> BankName.NH_NONGHYUP;
			case KAKAOBANK -> BankName.KAKAOBANK;
			case KB_KOOKMIN -> BankName.KB_KOOKMIN;
			case TOSSBANK -> BankName.TOSSBANK;
			case SHINHAN -> BankName.SHINHAN;
			case WOORI -> BankName.WOORI;
			case IBK_GIUP -> BankName.IBK_GIUP;
			case HANA -> BankName.HANA;
			case SAEMAUL -> BankName.SAEMAUL;
			case BUSAN -> BankName.BUSAN;
			case IMBANK_DAEGU -> BankName.IMBANK_DAEGU;
			case SINHYEOP -> BankName.SINHYEOP;
			case WOOCHAEGUK -> BankName.WOOCHAEGUK;
			case SCJEIL -> BankName.SCJEIL;
			case SUHYEOP -> BankName.SUHYEOP;
			case NONE -> BankName.NONE;
		};
	}

	public static BankNameType toApi(final BankName bankName) {
		if (bankName == null) {
			return null;
		}

		return switch (bankName) {
			case NH_NONGHYUP -> BankNameType.NH_NONGHYUP;
			case KAKAOBANK -> BankNameType.KAKAOBANK;
			case KB_KOOKMIN -> BankNameType.KB_KOOKMIN;
			case TOSSBANK -> BankNameType.TOSSBANK;
			case SHINHAN -> BankNameType.SHINHAN;
			case WOORI -> BankNameType.WOORI;
			case IBK_GIUP -> BankNameType.IBK_GIUP;
			case HANA -> BankNameType.HANA;
			case SAEMAUL -> BankNameType.SAEMAUL;
			case BUSAN -> BankNameType.BUSAN;
			case IMBANK_DAEGU -> BankNameType.IMBANK_DAEGU;
			case SINHYEOP -> BankNameType.SINHYEOP;
			case WOOCHAEGUK -> BankNameType.WOOCHAEGUK;
			case SCJEIL -> BankNameType.SCJEIL;
			case SUHYEOP -> BankNameType.SUHYEOP;
			case NONE -> BankNameType.NONE;
		};
	}
}
