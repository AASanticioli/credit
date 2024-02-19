package me.dio.credit.application.system.controller


import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import me.dio.credit.application.system.dto.request.CreditDto
import me.dio.credit.application.system.dto.request.CustomerDto
import me.dio.credit.application.system.dto.request.CustomerUpdateDto
import me.dio.credit.application.system.entity.Address
import me.dio.credit.application.system.entity.Credit
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.enummeration.Status
import me.dio.credit.application.system.repository.CreditRepository
import me.dio.credit.application.system.repository.CustomerRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ContextConfiguration
class CreditResourceTest {
    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var creditRepository: CreditRepository

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {
        private const val URL = "/api/credits"

        private const val CUSTOMER_FIRSTNAME = "Tomás Calebe"
        private const val CUSTOMER_LASTNAME = "Farias"
        private const val CUSTOMER_CPF = "517.429.568-07"
        private const val CUSTOMER_EMAIL = "tomas_farias@camarasjc.sp.gov.br"
        private const val CUSTOMER_INCOME = 1_000.0
        private const val CUSTOMER_PASSWORD = "JuIzRXHe1u"
        private const val CUSTOMER_ZIPCODE = "04880-033"
        private const val CUSTOMER_STREET = "Travessa Renascer, 806 - Recanto Campo Belo - São Paulo - SP"
        private const val CUSTOMER_NONEXISTENT_ID = 1000L

        private const val CREDIT_VALUE = 1_000.0
        private const val CREDIT_INSTALLMENT_MONTHS = 2L
        private const val CREDIT_INSTALLMENT_MONTHS_INVALID = 5L
        private const val CREDIT_INSTALLMENT_NUMBER = 24
        private const val CREDIT_INSTALLMENT_NUMBER_INVALID = 49
        private const val CREDIT_STATUS_IN_PROGRESS = "IN_PROGRESS"

    }

    private lateinit var customer: Customer

    @BeforeEach
    fun setup() {
        customerRepository.deleteAll()
        customer = customerRepository.save(makeCustomer())
    }

    @AfterEach
    fun tearDown() {
        customerRepository.deleteAll()
    }


    @Test
    fun `Should create a credit`() {
        //given
        val creditDto = makeCreditDto(customerId = customer.id!!)
        val creditDtoAsString = objectMapper.writeValueAsString(creditDto)
        val creditDtoRequestBuilder = MockMvcRequestBuilders.post(URL).contentType(MediaType.APPLICATION_JSON).content(creditDtoAsString)
        val statusExpected = MockMvcResultMatchers.status().isCreated

        //when + then
        mockMvc
            .perform(creditDtoRequestBuilder)
            .andExpect(statusExpected)
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditCode").isNotEmpty)
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditValue").value(CREDIT_VALUE))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(CREDIT_STATUS_IN_PROGRESS))
            .andExpect(MockMvcResultMatchers.jsonPath("$.emailCustomer").value(customer.email))
            .andDo(MockMvcResultHandlers.print())
    }


    @Test
    fun `should return 400 HTTP status (Bad Request) for invalid day of the First Installment`() {
        // given
        val creditDto = makeCreditDto(customerId = customer.id!!, dayFirstOfInstallment = LocalDate.now().plusMonths(CREDIT_INSTALLMENT_MONTHS_INVALID))
        val creditDtoAsString = objectMapper.writeValueAsString(creditDto)
        val creditDtoRequestBuilder = MockMvcRequestBuilders.post(URL).contentType(MediaType.APPLICATION_JSON).content(creditDtoAsString)
        val statusExpected = MockMvcResultMatchers.status().isBadRequest

        // when + then
        mockMvc
            .perform(creditDtoRequestBuilder)
            .andExpect(statusExpected)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.exception").value("class me.dio.credit.application.system.exception.BusinessException"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
            .andExpect(MockMvcResultMatchers.jsonPath("$.details.null").value("Invalid Date"))
            .andDo(MockMvcResultHandlers.print())
    }


    @Test
    fun `should return 400 HTTP status (Bad Request) for current date for the day of the First Installment`() {
        // given
        val creditDto = makeCreditDto(customerId = customer.id!!, dayFirstOfInstallment = LocalDate.now())
        val creditDtoAsString = objectMapper.writeValueAsString(creditDto)
        val creditDtoRequestBuilder = MockMvcRequestBuilders.post(URL).contentType(MediaType.APPLICATION_JSON).content(creditDtoAsString)
        val statusExpected = MockMvcResultMatchers.status().isBadRequest

        // when + then
        mockMvc
            .perform(creditDtoRequestBuilder)
            .andExpect(statusExpected)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.exception").value("class org.springframework.web.bind.MethodArgumentNotValidException"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.details.dayFirstOfInstallment").value("must be a future date"))
            .andDo(MockMvcResultHandlers.print())
    }


    @Test
    fun `should return 400 HTTP status (Bad Request) for number of installments greater than 48`() {
        // given
        val creditDto = makeCreditDto(customerId = customer.id!!, numberOfInstallments = CREDIT_INSTALLMENT_NUMBER_INVALID)
        val creditDtoAsString = objectMapper.writeValueAsString(creditDto)
        val creditDtoRequestBuilder = MockMvcRequestBuilders.post(URL).contentType(MediaType.APPLICATION_JSON).content(creditDtoAsString)
        val statusExpected = MockMvcResultMatchers.status().isBadRequest

        // when + then
        mockMvc
            .perform(creditDtoRequestBuilder)
            .andExpect(statusExpected)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.exception").value("class org.springframework.web.bind.MethodArgumentNotValidException"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.details.numberOfInstallments").value("must be less than or equal to 48"))
            .andDo(MockMvcResultHandlers.print())
    }


    @Test
    fun `should return 400 HTTP status (Bad Request) for invalid Customer Id`() {
        // given
        val creditDto = makeCreditDto(customerId = CUSTOMER_NONEXISTENT_ID)
        val creditDtoAsString = objectMapper.writeValueAsString(creditDto)
        val creditDtoRequestBuilder = MockMvcRequestBuilders.post(URL).contentType(MediaType.APPLICATION_JSON).content(creditDtoAsString)
        val statusExpected = MockMvcResultMatchers.status().isBadRequest

        // when + then
        mockMvc
            .perform(creditDtoRequestBuilder)
            .andExpect(statusExpected)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.exception").value("class me.dio.credit.application.system.exception.BusinessException"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
            .andExpect(MockMvcResultMatchers.jsonPath("$.details.null").value("Id $CUSTOMER_NONEXISTENT_ID not found"))
            .andDo(MockMvcResultHandlers.print())
    }


    @Test
    fun `should find credits by a Customer by Id`(){
        //given
        val credit1 = creditRepository.save(makeCredit())
        val credit2 = creditRepository.save(makeCredit())
        val credit3 = creditRepository.save(makeCredit())
        val creditDtoRequestBuilder = MockMvcRequestBuilders.get("$URL?customerId=  ${customer.id}").accept(MediaType.APPLICATION_JSON)
        val statusExpected = MockMvcResultMatchers.status().isOk
        //when + then
        mockMvc
            .perform(creditDtoRequestBuilder)
            .andExpect(statusExpected)
            .andExpect(MockMvcResultMatchers.jsonPath("$.[0].creditCode").value(credit1.creditCode.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.[1].creditCode").value(credit2.creditCode.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.[2].creditCode").value(credit3.creditCode.toString()))
            .andDo(MockMvcResultHandlers.print())
    }


    @Test
    fun `should find credit by code and customer id`() {
        //given
        val credit1 = creditRepository.save(makeCredit())
        val creditDtoRequestBuilder = MockMvcRequestBuilders.get("$URL/${credit1.creditCode}?customerId=${customer.id}").accept(MediaType.APPLICATION_JSON)
        val statusExpected = MockMvcResultMatchers.status().isOk
        //when + then
        mockMvc
            .perform(creditDtoRequestBuilder)
            .andExpect(statusExpected)
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditCode").value(credit1.creditCode.toString()))
            .andDo(MockMvcResultHandlers.print())
    }


    private fun makeCreditDto(
        creditValue: BigDecimal = BigDecimal.valueOf(CREDIT_VALUE),
        dayFirstOfInstallment: LocalDate = LocalDate.now().plusMonths(CREDIT_INSTALLMENT_MONTHS),
        numberOfInstallments: Int = CREDIT_INSTALLMENT_NUMBER,
        customerId: Long
    ): CreditDto{
        return CreditDto(
            creditValue = creditValue,
            dayFirstOfInstallment = dayFirstOfInstallment,
            numberOfInstallments = numberOfInstallments,
            customerId = customerId
        )
    }


    private fun makeCredit(
        creditValue: BigDecimal = BigDecimal.valueOf(CREDIT_VALUE),
        dayFirstInstallment: LocalDate = LocalDate.now().plusMonths(CREDIT_INSTALLMENT_MONTHS),
        numberOfInstallments: Int = CREDIT_INSTALLMENT_NUMBER,
    ): Credit {
        return Credit(
            creditValue = creditValue,
            dayFirstInstallment = dayFirstInstallment,
            numberOfInstallments = numberOfInstallments,
            customer = customer,
        )
    }

    private fun makeCustomer(
        firstName: String = CUSTOMER_FIRSTNAME,
        lastName: String = CUSTOMER_LASTNAME,
        cpf: String = CUSTOMER_CPF,
        email: String = CUSTOMER_EMAIL,
        income: BigDecimal = BigDecimal.valueOf(CUSTOMER_INCOME),
        password: String = CUSTOMER_PASSWORD,
        zipCode: String = CUSTOMER_ZIPCODE,
        street: String = CUSTOMER_STREET,
    ): Customer {
        return Customer(
            firstName = firstName,
            lastName = lastName,
            cpf = cpf,
            email = email,
            income = income,
            password = password,
            address = Address(zipCode = zipCode, street = street),
        )
    }

    private fun makeCustomerDto(
        firstName: String = CUSTOMER_FIRSTNAME,
        lastName: String = CUSTOMER_LASTNAME,
        cpf: String = CUSTOMER_CPF,
        email: String = CUSTOMER_EMAIL,
        income: BigDecimal = BigDecimal.valueOf(CUSTOMER_INCOME),
        password: String = CUSTOMER_PASSWORD,
        zipCode: String = CUSTOMER_ZIPCODE,
        street: String = CUSTOMER_STREET,
    ): CustomerDto {
        return CustomerDto(
            firstName = firstName,
            lastName = lastName,
            cpf = cpf,
            income = income,
            email = email,
            password = password,
            zipCode = zipCode,
            street = street
        )
    }
}