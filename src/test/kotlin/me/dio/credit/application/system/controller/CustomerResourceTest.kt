package me.dio.credit.application.system.controller

import com.fasterxml.jackson.databind.ObjectMapper
import me.dio.credit.application.system.dto.request.CustomerDto
import me.dio.credit.application.system.dto.request.CustomerUpdateDto
import me.dio.credit.application.system.repository.CustomerRepository
import org.junit.jupiter.api.AfterEach
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

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ContextConfiguration
class CustomerResourceTest {
    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {
        private const val URL = "/api/customers"

        private const val CUSTOMER_FIRSTNAME = "Tomás Calebe"
        private const val CUSTOMER_LASTNAME = "Farias"
        private const val CUSTOMER_CPF = "517.429.568-07"
        private const val CUSTOMER_EMAIL = "tomas_farias@camarasjc.sp.gov.br"
        private const val CUSTOMER_INCOME = 1_000.0
        private const val CUSTOMER_PASSWORD = "JuIzRXHe1u"
        private const val CUSTOMER_ZIPCODE = "04880-033"
        private const val CUSTOMER_STREET = "Travessa Renascer, 806 - Recanto Campo Belo - São Paulo - SP"

        private const val CUSTOMER_FIRSTNAME_UPDATED = "$CUSTOMER_FIRSTNAME «updated»"
        private const val CUSTOMER_LASTNAME_UPDATED = "$CUSTOMER_LASTNAME «updated»"
        private const val CUSTOMER_INCOME_UPDATED = CUSTOMER_INCOME * 2
        private const val CUSTOMER_ZIPCODE_UPDATED = "15991-144"
        private const val CUSTOMER_STREET_UPDATED = "$CUSTOMER_STREET «updated»"

    }

    @BeforeEach
    fun setup() {
        customerRepository.deleteAll()
    }

    @AfterEach
    fun tearDown() {
        customerRepository.deleteAll()
    }

    @Test
    fun `Should create a customer`() {
        //given
        val customerDto = makeCustomerDto()
        val customerDtoAsString = objectMapper.writeValueAsString(customerDto)
        val customerDtoRequestBuilder = MockMvcRequestBuilders.post(URL).contentType(MediaType.APPLICATION_JSON).content(customerDtoAsString)
        val statusExpected = MockMvcResultMatchers.status().isCreated

        //when + then
        mockMvc
            .perform(customerDtoRequestBuilder)
            .andExpect(statusExpected)
            .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value(CUSTOMER_FIRSTNAME))
            .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value(CUSTOMER_LASTNAME))
            .andExpect(MockMvcResultMatchers.jsonPath("$.cpf").value(CUSTOMER_CPF))
            .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(CUSTOMER_EMAIL))
            .andExpect(MockMvcResultMatchers.jsonPath("$.zipCode").value(CUSTOMER_ZIPCODE))
            .andExpect(MockMvcResultMatchers.jsonPath("$.street").value(CUSTOMER_STREET))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").isNotEmpty)
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should return 409 HTTP status (conflict) for duplicated cpf creation`() {
        // given
        val customerDto = makeCustomerDto()
        val customerDtoAsString = objectMapper.writeValueAsString(customerDto)
        val customerDtoRequestBuilder = MockMvcRequestBuilders.post(URL).contentType(MediaType.APPLICATION_JSON).content(customerDtoAsString)
        val statusExpected = MockMvcResultMatchers.status().isConflict
        customerRepository.save(customerDto.toEntity())

        // when + then
        mockMvc
            .perform(customerDtoRequestBuilder)
            .andExpect(statusExpected)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Conflict! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.exception").value("class org.springframework.dao.DataIntegrityViolationException"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should return 400 HTTP status (bad request) for empty firstName creation`() {
        //given
        val customerDto = makeCustomerDto(firstName = "")
        val customerDtoAsString = objectMapper.writeValueAsString(customerDto)
        val customerDtoRequestBuilder = MockMvcRequestBuilders.post(URL).contentType(MediaType.APPLICATION_JSON).content(customerDtoAsString)
        val statusExpected = MockMvcResultMatchers.status().isBadRequest

        //when + then
        mockMvc
            .perform(customerDtoRequestBuilder)
            .andExpect(statusExpected)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.exception").value("class org.springframework.web.bind.MethodArgumentNotValidException"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should find customer by ID`() {
        //given
        val customerDto = makeCustomerDto()
        val customer = customerRepository.save(customerDto.toEntity())
        val customerDtoRequestBuilder = MockMvcRequestBuilders.get("$URL/${customer.id}").accept(MediaType.APPLICATION_JSON)
        val statusExpected = MockMvcResultMatchers.status().isOk

        //when + then
        mockMvc
            .perform(customerDtoRequestBuilder)
            .andExpect(statusExpected)
            .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value(CUSTOMER_FIRSTNAME))
            .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value(CUSTOMER_LASTNAME))
            .andExpect(MockMvcResultMatchers.jsonPath("$.cpf").value(CUSTOMER_CPF))
            .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(CUSTOMER_EMAIL))
            .andExpect(MockMvcResultMatchers.jsonPath("$.zipCode").value(CUSTOMER_ZIPCODE))
            .andExpect(MockMvcResultMatchers.jsonPath("$.street").value(CUSTOMER_STREET))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").isNotEmpty)
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should return bad request (400) when find by an nonexistent id`() {
        //given
        val nonexistentId = -1L
        val customerDtoRequestBuilder = MockMvcRequestBuilders.get("$URL/$nonexistentId").accept(MediaType.APPLICATION_JSON)
        val statusExpected = MockMvcResultMatchers.status().isBadRequest
        //when + then
        mockMvc.perform(customerDtoRequestBuilder)
            .andExpect(statusExpected)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.exception").value("class me.dio.credit.application.system.exception.BusinessException"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should delete customer by id`() {
        //given
        val customerDto = makeCustomerDto()
        val customer = customerRepository.save(customerDto.toEntity())
        val customerDtoRequestBuilder = MockMvcRequestBuilders.delete("$URL/${customer.id}").accept(MediaType.APPLICATION_JSON)
        val statusExpected = MockMvcResultMatchers.status().isNoContent
        //when + then
        mockMvc.perform(customerDtoRequestBuilder)
            .andExpect(statusExpected)
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should return BadRequest when delete a customer by a nonexistent id`() {
        //given
        val nonexistentId = -1L
        val customerDtoRequestBuilder = MockMvcRequestBuilders.delete("$URL/$nonexistentId").accept(MediaType.APPLICATION_JSON)
        val statusExpected = MockMvcResultMatchers.status().isBadRequest
        //when + then
        mockMvc.perform(customerDtoRequestBuilder)
            .andExpect(statusExpected)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.exception").value("class me.dio.credit.application.system.exception.BusinessException"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should update a Customer by id`(){
        //given
        val customerDto = makeCustomerDto()
        val customer = customerRepository.save(customerDto.toEntity())
        val customerUpdateDto = makeCustomerUpdateDto()
        val customerUpdateDtoAsString = objectMapper.writeValueAsString(customerUpdateDto)
        val customerDtoRequestBuilder = MockMvcRequestBuilders.patch("$URL?customerId=${customer.id}")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(customerUpdateDtoAsString)
        val statusExpected = MockMvcResultMatchers.status().isOk
        //when + then
        mockMvc.perform(customerDtoRequestBuilder)
            .andExpect(statusExpected)
            .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value(CUSTOMER_FIRSTNAME_UPDATED))
            .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value(CUSTOMER_LASTNAME_UPDATED))
            .andExpect(MockMvcResultMatchers.jsonPath("$.cpf").value(CUSTOMER_CPF))
            .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(CUSTOMER_EMAIL))
            .andExpect(MockMvcResultMatchers.jsonPath("$.zipCode").value(CUSTOMER_ZIPCODE_UPDATED))
            .andExpect(MockMvcResultMatchers.jsonPath("$.street").value(CUSTOMER_STREET_UPDATED))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").isNotEmpty)
    }

    @Test
    fun `should return BadRequest when update a customer by a nonexistent id`() {
        //given
        val nonexistentId = -1L
        val customerUpdateDto = makeCustomerUpdateDto()
        val customerUpdateDtoAsString = objectMapper.writeValueAsString(customerUpdateDto)
        val customerDtoRequestBuilder = MockMvcRequestBuilders.patch("$URL?customerId=$nonexistentId")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(customerUpdateDtoAsString)
        val statusExpected = MockMvcResultMatchers.status().isBadRequest
        //when + then
        mockMvc.perform(customerDtoRequestBuilder)
            .andExpect(statusExpected)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.exception").value("class me.dio.credit.application.system.exception.BusinessException"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
            .andDo(MockMvcResultHandlers.print())
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


    private fun makeCustomerUpdateDto(
        firstName: String = CUSTOMER_FIRSTNAME_UPDATED,
        lastName: String = CUSTOMER_LASTNAME_UPDATED,
        income: BigDecimal = BigDecimal.valueOf(CUSTOMER_INCOME_UPDATED),
        zipCode: String = CUSTOMER_ZIPCODE_UPDATED,
        street: String = CUSTOMER_STREET_UPDATED,
    ): CustomerUpdateDto {
        return CustomerUpdateDto(
            firstName = firstName,
            lastName = lastName,
            income = income,
            zipCode = zipCode,
            street = street
        )
    }
}