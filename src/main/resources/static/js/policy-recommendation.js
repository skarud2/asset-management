document.addEventListener("DOMContentLoaded", () => {

    // ============================================
    // 대한민국 시·도 / 시·군·구 데이터
    // ============================================
    const regionData = {

        "서울특별시": [
            "종로구", "중구", "용산구", "성동구", "광진구",
            "동대문구", "중랑구", "성북구", "강북구", "도봉구",
            "노원구", "은평구", "서대문구", "마포구", "양천구",
            "강서구", "구로구", "금천구", "영등포구", "동작구",
            "관악구", "서초구", "강남구", "송파구", "강동구"
        ],

        "부산광역시": [
            "중구", "서구", "동구", "영도구", "부산진구",
            "동래구", "남구", "북구", "해운대구", "사하구",
            "금정구", "강서구", "연제구", "수영구", "사상구",
            "기장군"
        ],

        "대구광역시": [
            "중구", "동구", "서구", "남구", "북구",
            "수성구", "달서구", "달성군", "군위군"
        ],

        "인천광역시": [
            "중구", "동구", "미추홀구", "연수구", "남동구",
            "부평구", "계양구", "서구", "강화군", "옹진군"
        ],

        "광주광역시": [
            "동구", "서구", "남구", "북구", "광산구"
        ],

        "대전광역시": [
            "동구", "중구", "서구", "유성구", "대덕구"
        ],

        "울산광역시": [
            "중구", "남구", "동구", "북구", "울주군"
        ],

        "세종특별자치시": [
            "세종특별자치시"
        ],

        "경기도": [
            "수원시", "용인시", "고양시", "화성시", "성남시",
            "부천시", "남양주시", "안산시", "평택시", "안양시",
            "시흥시", "파주시", "김포시", "의정부시", "광주시",
            "하남시", "광명시", "군포시", "양주시", "오산시",
            "이천시", "안성시", "구리시", "의왕시", "포천시",
            "양평군", "여주시", "동두천시", "과천시", "가평군",
            "연천군"
        ],

        "강원특별자치도": [
            "춘천시", "원주시", "강릉시", "동해시", "태백시",
            "속초시", "삼척시", "홍천군", "횡성군", "영월군",
            "평창군", "정선군", "철원군", "화천군", "양구군",
            "인제군", "고성군", "양양군"
        ],

        "충청북도": [
            "청주시", "충주시", "제천시", "보은군", "옥천군",
            "영동군", "증평군", "진천군", "괴산군", "음성군",
            "단양군"
        ],

        "충청남도": [
            "천안시", "공주시", "보령시", "아산시", "서산시",
            "논산시", "계룡시", "당진시", "금산군", "부여군",
            "서천군", "청양군", "홍성군", "예산군", "태안군"
        ],

        "전북특별자치도": [
            "전주시", "군산시", "익산시", "정읍시", "남원시",
            "김제시", "완주군", "진안군", "무주군", "장수군",
            "임실군", "순창군", "고창군", "부안군"
        ],

        "전라남도": [
            "목포시", "여수시", "순천시", "나주시", "광양시",
            "담양군", "곡성군", "구례군", "고흥군", "보성군",
            "화순군", "장흥군", "강진군", "해남군", "영암군",
            "무안군", "함평군", "영광군", "장성군", "완도군",
            "진도군", "신안군"
        ],

        "경상북도": [
            "포항시", "경주시", "김천시", "안동시", "구미시",
            "영주시", "영천시", "상주시", "문경시", "경산시",
            "의성군", "청송군", "영양군", "영덕군", "청도군",
            "고령군", "성주군", "칠곡군", "예천군", "봉화군",
            "울진군", "울릉군"
        ],

        "경상남도": [
            "창원시", "진주시", "통영시", "사천시", "김해시",
            "밀양시", "거제시", "양산시", "의령군", "함안군",
            "창녕군", "고성군", "남해군", "하동군", "산청군",
            "함양군", "거창군", "합천군"
        ],

        "제주특별자치도": [
            "제주시", "서귀포시"
        ]
    };


    // ============================================
    // 화면 요소
    // ============================================
    const form =
        document.getElementById("recommendationForm");

    if (!form) {
        return;
    }


    const submitButton =
        document.getElementById("submitButton");

    const errorArea =
        document.getElementById("errorArea");


    // 지역
    const residenceSido =
        document.getElementById("residenceSido");

    const residenceSigungu =
        document.getElementById("residenceSigungu");


    // 자녀
    const childrenCountArea =
        document.getElementById("childrenCountArea");


    // 금융지원 목적
    const desiredSupportPurpose =
        document.getElementById("desiredSupportPurpose");

    const desiredAmountArea =
        document.getElementById("desiredAmountArea");

    const savingCapacityArea =
        document.getElementById("savingCapacityArea");


    // 고용형태는 금융프로필을 단일 기준으로 사용한다.
    const financialEmploymentStatus =
        document.querySelector(".recommendation-page")
            ?.dataset.employmentStatus || "";

    const employmentMonthsArea =
        document.getElementById("employmentMonthsArea");

    const incomeVerifiableArea =
        document.getElementById("incomeVerifiableArea");

    const welfareNone =
        document.getElementById("welfareNone");

    const welfareOptions =
        document.querySelectorAll(".welfare-option");


    // ============================================
    // 시·군·구 목록 변경
    // ============================================
    function updateSigunguOptions(
        sido,
        selectedSigungu = null
    ) {

        residenceSigungu.innerHTML = "";


        // 시·도 미선택
        if (!sido || !regionData[sido]) {

            const option =
                document.createElement("option");

            option.value = "";
            option.textContent =
                "먼저 시·도를 선택해 주세요";

            residenceSigungu.appendChild(option);

            residenceSigungu.disabled = true;

            return;
        }


        // 시·도 선택됨
        residenceSigungu.disabled = false;


        const defaultOption =
            document.createElement("option");

        defaultOption.value = "";
        defaultOption.textContent =
            "시·군·구를 선택해 주세요";

        residenceSigungu.appendChild(defaultOption);


        regionData[sido].forEach(sigungu => {

            const option =
                document.createElement("option");

            option.value = sigungu;
            option.textContent = sigungu;


            if (
                selectedSigungu !== null
                && sigungu === selectedSigungu
            ) {

                option.selected = true;
            }


            residenceSigungu.appendChild(option);
        });
    }


    // 시·도 변경
    residenceSido.addEventListener(
        "change",
        () => {

            updateSigunguOptions(
                residenceSido.value
            );
        }
    );


    // 소득이 있는 경우에만 소득증빙 가능 여부를 묻습니다.
    document.querySelectorAll('input[name="hasIncome"]').forEach(radio => {
        radio.addEventListener("change", updateIncomeVerifiableArea);
    });

    function updateIncomeVerifiableArea() {
        const hasIncome = getRadioValue("hasIncome") === "true";
        const radios = incomeVerifiableArea.querySelectorAll(
            'input[name="incomeVerifiable"]'
        );

        incomeVerifiableArea.hidden = !hasIncome;
        radios.forEach(radio => {
            radio.required = hasIncome;
            if (!hasIncome) {
                radio.checked = false;
            }
        });
    }


    // 복지 자격과 '해당 없음'은 동시에 선택할 수 없습니다.
    welfareOptions.forEach(option => {
        option.addEventListener("change", () => {
            if (option.checked) {
                welfareNone.checked = false;
                updateWelfareOptionState();
            }
        });
    });

    welfareNone.addEventListener("change", () => {
        if (welfareNone.checked) {
            welfareOptions.forEach(option => {
                option.checked = false;
            });
        }
        updateWelfareOptionState();
    });

    function updateWelfareNone() {
        welfareNone.checked = !Array.from(welfareOptions)
            .some(option => option.checked);
        updateWelfareOptionState();
    }

    function updateWelfareOptionState() {
        welfareOptions.forEach(option => {
            option.disabled = welfareNone.checked;
        });
    }

    function validateWelfareSelection() {
        const selected = welfareNone.checked
            || Array.from(welfareOptions).some(option => option.checked);

        if (selected) {
            return true;
        }

        errorArea.textContent =
            "복지 자격 항목을 선택하거나 ‘해당 없음’을 선택해 주세요.";
        welfareNone.focus();
        return false;
    }


    // ============================================
    // 자녀 여부에 따라 자녀 수 표시
    // ============================================
    document
        .querySelectorAll(
            'input[name="hasChildren"]'
        )
        .forEach(radio => {

            radio.addEventListener(
                "change",
                () => {

                    updateChildrenArea();
                }
            );
        });


    function updateChildrenArea() {

        const value =
            getRadioValue("hasChildren");

        const childrenCount =
            document.getElementById(
                "childrenCount"
            );


        if (value === "true") {

            childrenCountArea.style.display =
                "block";

        } else {

            childrenCountArea.style.display =
                "none";

            childrenCount.value = "";
        }
    }


    // ============================================
    // 지원 목적에 따라 금액 입력 표시
    // ============================================
    desiredSupportPurpose.addEventListener(
        "change",
        () => {

            updatePurposeFields();
        }
    );


    function updatePurposeFields() {

        const purpose =
            desiredSupportPurpose.value;


        const desiredAmount =
            document.getElementById(
                "desiredAmount"
            );

        const monthlySavingCapacity =
            document.getElementById(
                "monthlySavingCapacity"
            );


        // 자산형성
        if (purpose === "ASSET") {

            desiredAmountArea.style.display =
                "none";

            savingCapacityArea.style.display =
                "block";

            desiredAmount.value = "";

            return;
        }


        // 잘 모르겠음 / 미선택
        if (
            purpose === "UNKNOWN"
            || purpose === ""
        ) {

            desiredAmountArea.style.display =
                "none";

            savingCapacityArea.style.display =
                "none";

            desiredAmount.value = "";
            monthlySavingCapacity.value = "";

            return;
        }


        // 대출·지원 목적
        desiredAmountArea.style.display =
            "block";

        savingCapacityArea.style.display =
            "none";

        monthlySavingCapacity.value = "";
    }


    // ============================================
    // 무직/학생이면 재직기간 숨김
    // ============================================
    function updateEmploymentFields() {

        const value = financialEmploymentStatus;

        const employmentMonths =
            document.getElementById(
                "employmentMonths"
            );


        if (
            value === "UNEMPLOYED"
            || value === "STUDENT"
            || value === ""
        ) {

            employmentMonthsArea.style.display =
                "none";

            employmentMonths.value = "";

            return;
        }


        employmentMonthsArea.style.display =
            "block";
    }


    // ============================================
    // 기존 설문 조회
    // ============================================
    async function loadProfile() {

        try {

            const response =
                await fetch(
                    "/api/policy/recommendation",
                    {
                        method: "GET",
                        headers: {
                            "Accept":
                                "application/json"
                        }
                    }
                );


            if (!response.ok) {

                console.warn(
                    "기존 추천 프로필을 조회하지 못했습니다."
                );

                return;
            }


            /*
             * ResponseEntity.ok(null)인 경우처럼
             * 응답 본문이 없을 수도 있으므로
             * 먼저 text로 확인
             */
            const responseText =
                await response.text();


            if (!responseText) {
                return;
            }


            const data =
                JSON.parse(responseText);


            if (!data) {
                return;
            }


            // =========================
            // STEP 1
            // =========================
            setValue(
                "residenceSido",
                data.residenceSido
            );

            updateSigunguOptions(
                data.residenceSido,
                data.residenceSigungu
            );

            setValue(
                "residenceMonths",
                data.residenceMonths
            );


            // =========================
            // STEP 2
            // =========================
            setValue(
                "employmentMonths",
                data.employmentMonths
            );

            setRadio(
                "hasIncome",
                data.hasIncome
            );

            setRadio(
                "incomeVerifiable",
                data.incomeVerifiable
            );

            setValue(
                "householdAnnualIncome",
                data.householdAnnualIncome
            );

            setValue(
                "householdNetAssetAmount",
                data.householdNetAssetAmount
            );

            updateIncomeVerifiableArea();


            // =========================
            // STEP 3
            // =========================
            setValue(
                "householdSize",
                data.householdSize
            );

            setValue(
                "maritalStatus",
                data.maritalStatus
            );

            setRadio(
                "homelessHousehold",
                data.homelessHousehold
            );

            setRadio(
                "householdHead",
                data.householdHead
            );

            setRadio(
                "prospectiveHouseholdHead",
                data.prospectiveHouseholdHead
            );

            setRadio(
                "firstTimeHomeBuyer",
                data.firstTimeHomeBuyer
            );

            setRadio(
                "hasChildren",
                data.hasChildren
            );

            setValue(
                "childrenCount",
                data.childrenCount
            );


            setCheckbox(
                "basicLivelihoodRecipient",
                data.basicLivelihoodRecipient
            );

            setCheckbox(
                "nearPoverty",
                data.nearPoverty
            );

            setCheckbox(
                "singleParentHousehold",
                data.singleParentHousehold
            );

            setCheckbox(
                "disabled",
                data.disabled
            );

            setCheckbox(
                "selfRelianceYouth",
                data.selfRelianceYouth
            );

            setCheckbox(
                "multiculturalHousehold",
                data.multiculturalHousehold
            );

            setCheckbox(
                "northKoreanDefector",
                data.northKoreanDefector
            );

            setCheckbox(
                "childHeadedHousehold",
                data.childHeadedHousehold
            );

            setCheckbox(
                "earnedIncomeTaxCreditRecipient",
                data.earnedIncomeTaxCreditRecipient
            );

            setCheckbox(
                "basicPensionRecipient",
                data.basicPensionRecipient
            );

            setCheckbox(
                "disabilityBenefitRecipient",
                data.disabilityBenefitRecipient
            );

            setCheckbox(
                "jeonseFraudVictim",
                data.jeonseFraudVictim
            );

            updateWelfareNone();


            // =========================
            // STEP 4
            // =========================
            setRadio(
                "debtDefaultStatus",
                data.debtDefaultStatus
            );

            setRadio(
                "overdueStatus",
                data.overdueStatus
            );

            setValue(
                "policyFinanceUsage",
                data.policyFinanceUsage
            );

            setRadio(
                "financialEducationStatus",
                data.financialEducationStatus
            );


            // =========================
            // STEP 5
            // =========================
            setValue(
                "desiredSupportPurpose",
                data.desiredSupportPurpose
            );

            setValue(
                "desiredAmount",
                data.desiredAmount
            );

            setValue(
                "monthlySavingCapacity",
                data.monthlySavingCapacity
            );

            setValue(
                "priorityPreference",
                data.priorityPreference
            );


            // 조건부 영역 갱신
            updateChildrenArea();
            updatePurposeFields();
            updateEmploymentFields();


        } catch (error) {

            console.error(
                "추천 프로필 조회 오류:",
                error
            );
        }
    }


    // ============================================
    // 설문 저장
    // ============================================
    form.addEventListener(
        "submit",
        async event => {

            event.preventDefault();

            errorArea.textContent = "";

            if (!validateWelfareSelection()) {
                return;
            }


            const request = {

                // =====================
                // 거주
                // =====================
                residenceSido:
                    getValue("residenceSido"),

                residenceSigungu:
                    getValue("residenceSigungu"),

                residenceMonths:
                    getNumber("residenceMonths"),


                // =====================
                // 근로/소득
                // =====================
                employmentMonths:
                    getNumber("employmentMonths"),

                hasIncome:
                    getBooleanRadio("hasIncome"),

                incomeVerifiable:
                    getBooleanRadio("hasIncome") === true
                        ? getRadioValue("incomeVerifiable")
                        : null,

                householdAnnualIncome:
                    getNumber("householdAnnualIncome"),

                householdNetAssetAmount:
                    getNumber("householdNetAssetAmount"),


                // =====================
                // 가구
                // =====================
                householdSize:
                    getNumber("householdSize"),

                maritalStatus:
                    getValue("maritalStatus"),

                homelessHousehold:
                    getBooleanRadio("homelessHousehold"),

                householdHead:
                    getBooleanRadio("householdHead"),

                prospectiveHouseholdHead:
                    getBooleanRadio("prospectiveHouseholdHead"),

                firstTimeHomeBuyer:
                    getBooleanRadio("firstTimeHomeBuyer"),

                hasChildren:
                    getBooleanRadio("hasChildren"),

                childrenCount:
                    getNumber("childrenCount"),


                // =====================
                // 복지
                // =====================
                basicLivelihoodRecipient:
                    getCheckbox(
                        "basicLivelihoodRecipient"
                    ),

                nearPoverty:
                    getCheckbox(
                        "nearPoverty"
                    ),

                singleParentHousehold:
                    getCheckbox(
                        "singleParentHousehold"
                    ),

                disabled:
                    getCheckbox(
                        "disabled"
                    ),

                selfRelianceYouth:
                    getCheckbox(
                        "selfRelianceYouth"
                    ),

                multiculturalHousehold:
                    getCheckbox(
                        "multiculturalHousehold"
                    ),

                northKoreanDefector:
                    getCheckbox(
                        "northKoreanDefector"
                    ),

                childHeadedHousehold:
                    getCheckbox(
                        "childHeadedHousehold"
                    ),

                earnedIncomeTaxCreditRecipient:
                    getCheckbox(
                        "earnedIncomeTaxCreditRecipient"
                    ),

                basicPensionRecipient:
                    getCheckbox(
                        "basicPensionRecipient"
                    ),

                disabilityBenefitRecipient:
                    getCheckbox(
                        "disabilityBenefitRecipient"
                    ),

                jeonseFraudVictim:
                    getCheckbox(
                        "jeonseFraudVictim"
                    ),


                // =====================
                // 신용/금융
                // =====================
                debtDefaultStatus:
                    getRadioValue(
                        "debtDefaultStatus"
                    ),

                overdueStatus:
                    getRadioValue(
                        "overdueStatus"
                    ),

                policyFinanceUsage:
                    getValue(
                        "policyFinanceUsage"
                    ),

                financialEducationStatus:
                    getRadioValue(
                        "financialEducationStatus"
                    ),


                // =====================
                // 희망지원
                // =====================
                desiredSupportPurpose:
                    getValue(
                        "desiredSupportPurpose"
                    ),

                desiredAmount:
                    getNumber(
                        "desiredAmount"
                    ),

                monthlySavingCapacity:
                    getNumber(
                        "monthlySavingCapacity"
                    ),

                priorityPreference:
                    getValue(
                        "priorityPreference"
                    )
            };


            try {

                submitButton.disabled = true;

                submitButton.textContent =
                    "분석 준비 중...";


                const response =
                    await fetch(
                        "/api/policy/recommendation",
                        {
                            method: "POST",

                            headers: {
                                "Content-Type":
                                    "application/json",

                                "Accept":
                                    "application/json"
                            },

                            body:
                                JSON.stringify(request)
                        }
                    );


                if (!response.ok) {

                    throw new Error(
                        "설문 저장에 실패했습니다."
                    );
                }


                // 추천 결과 화면으로 이동
                window.location.href =
                    "/policy/recommendation/result";


            } catch (error) {

                console.error(
                    "설문 저장 오류:",
                    error
                );

                errorArea.textContent =
                    error.message;

            } finally {

                submitButton.disabled = false;

                submitButton.textContent =
                    "맞춤상품 찾기";
            }
        }
    );


    // ============================================
    // 공통 : 값 가져오기
    // ============================================
    function getValue(id) {

        const element =
            document.getElementById(id);

        if (!element) {
            return null;
        }


        const value =
            element.value.trim();


        return value === ""
            ? null
            : value;
    }


    // ============================================
    // 공통 : 숫자 가져오기
    // ============================================
    function getNumber(id) {

        const value =
            getValue(id);


        if (value === null) {
            return null;
        }


        const number =
            Number(value);


        return Number.isFinite(number)
            ? number
            : null;
    }


    // ============================================
    // 공통 : 라디오 값
    // ============================================
    function getRadioValue(name) {

        const checked =
            document.querySelector(
                `input[name="${name}"]:checked`
            );


        return checked
            ? checked.value
            : null;
    }


    // ============================================
    // 공통 : Boolean 라디오
    // ============================================
    function getBooleanRadio(name) {

        const value =
            getRadioValue(name);


        if (value === null) {
            return null;
        }


        return value === "true";
    }


    // ============================================
    // 공통 : 체크박스
    // ============================================
    function getCheckbox(id) {

        const element =
            document.getElementById(id);


        return element
            ? element.checked
            : false;
    }


    // ============================================
    // 공통 : 값 세팅
    // ============================================
    function setValue(id, value) {

        const element =
            document.getElementById(id);


        if (!element) {
            return;
        }


        element.value =
            value ?? "";
    }


    // ============================================
    // 공통 : 체크박스 세팅
    // ============================================
    function setCheckbox(id, value) {

        const element =
            document.getElementById(id);


        if (!element) {
            return;
        }


        element.checked =
            value === true;
    }


    // ============================================
    // 공통 : 라디오 세팅
    // ============================================
    function setRadio(name, value) {

        if (
            value === null
            || value === undefined
        ) {
            return;
        }


        const radioValue =
            typeof value === "boolean"
                ? String(value)
                : String(value);


        const radios =
            document.querySelectorAll(
                `input[name="${name}"]`
            );


        radios.forEach(radio => {

            radio.checked =
                radio.value === radioValue;
        });
    }


    // ============================================
    // 설문 상단으로 이동
    // ============================================
    function scrollToSurveyTop() {

        const top =
            document.querySelector(
                ".recommendation-header"
            );


        if (!top) {
            return;
        }


        top.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    }


    // ============================================
    // 최초 화면 설정
    // ============================================

    // 지역 초기화
    updateSigunguOptions(null);


    // 조건부 필드 초기화
    updateChildrenArea();
    updatePurposeFields();
    updateEmploymentFields();
    updateIncomeVerifiableArea();
    updateWelfareNone();


    // 기존 저장 설문 불러오기
    loadProfile();
});
