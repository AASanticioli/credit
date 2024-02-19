package me.dio.credit.application.system.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import me.dio.credit.application.system.entity.Address
import me.dio.credit.application.system.entity.Credit
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.exception.BusinessException
import me.dio.credit.application.system.repository.CreditRepository
import me.dio.credit.application.system.enummeration.Status
import me.dio.credit.application.system.service.impl.CreditService
import me.dio.credit.application.system.service.impl.CustomerService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class CreditServiceTest {
    companion object{
        private const val CUSTOMER_ID = 1L
        private const val CUSTOMER_ID_NONEXISTENT = 2L
        private const val CUSTOMER_NAME = "Tomás Calebe"
        private const val CUSTOMER_LAST_NAME = "Farias"
        private const val CUSTOMER_CPF = "517.429.568-07"
        private const val CUSTOMER_EMAIL = "tomas_farias@camarasjc.sp.gov.br"
        private const val CUSTOMER_INCOME = 1_000L
        private const val CUSTOMER_PASSWORD = "JuIzRXHe1u"
        private const val CUSTOMER_ZIP_CODE = "04880-033"
        private const val CUSTOMER_STREET = "Travessa Renascer, 806 - Recanto Campo Belo - São Paulo - SP"

        private const val CREDIT_VALUE = 1_000.0
        private const val CREDIT_INSTALLMENT_MONTHS = 2L
        private const val CREDIT_INSTALLMENT_MONTHS_INVALID = 5L
        private const val CREDIT_INSTALLMENT_NUMBER = 24
    }

    @MockK
    lateinit var creditRepository: CreditRepository

    @MockK
    lateinit var customerService: CustomerService

    @InjectMockKs
    lateinit var creditService: CreditService

    @Test
    fun `should crate a credit`() {
        //given
        val fakeCredit = makeCredit()
        every { customerService.findById(CUSTOMER_ID) } returns fakeCredit.customer!!
        every { creditRepository.save(any()) } returns fakeCredit

        //when
        val savedFakeCredit = creditService.save(fakeCredit)

        //then
        verify(exactly = 1) { customerService.findById(CUSTOMER_ID) }
        verify(exactly = 1) { creditRepository.save(fakeCredit) }

        Assertions.assertThat(savedFakeCredit).isNotNull
        Assertions.assertThat(savedFakeCredit).isSameAs(fakeCredit)
    }

    @Test
    fun `Should throw an BusinessException for Invalid Day of First Installment`() {
        //given
        val invalidDayFirstInstallment: LocalDate = LocalDate.now().plusMonths(CREDIT_INSTALLMENT_MONTHS_INVALID)
        val credit: Credit = makeCredit(dayFirstInstallment = invalidDayFirstInstallment)
        every { creditRepository.save(credit) } answers { credit }

        //when
        Assertions.assertThatThrownBy { creditService.save(credit) }
            .isInstanceOf(BusinessException::class.java)
            .hasMessage("Invalid Date")

        //then
        verify(exactly = 0) { creditRepository.save(any()) }
    }

    @Test
    fun `should find credits by a Customer by Id`() {
        //given
        val expectedCredits: List<Credit> = listOf(makeCredit(), makeCredit(), makeCredit())
        every { creditRepository.findAllByCustomerId(CUSTOMER_ID) } returns expectedCredits

        //when
        val actual: List<Credit> = creditService.findAllByCustomer(CUSTOMER_ID)

        //then
        Assertions.assertThat(actual).isNotNull
        Assertions.assertThat(actual).isSameAs(expectedCredits)
        verify(exactly = 1) { creditRepository.findAllByCustomerId(CUSTOMER_ID) }
    }

    @Test
    fun `should find a Credit by code and customer id`() {
        //given
        val creditCode: UUID = UUID.randomUUID()
        val credit: Credit = makeCredit(customer = Customer(id = CUSTOMER_ID))
        every { creditRepository.findByCreditCode(creditCode) } returns credit

        //when
        val actual: Credit = creditService.findByCreditCode(CUSTOMER_ID, creditCode)

        //then
        Assertions.assertThat(actual).isNotNull
        Assertions.assertThat(actual).isSameAs(credit)
        verify(exactly = 1) { creditRepository.findByCreditCode(creditCode) }
    }

    @Test
    fun `should throw a Business Exception when find a Credit by an invalid code`() {
        //given
        val invalidCreditCode: UUID = UUID.randomUUID()
        every { creditRepository.findByCreditCode(invalidCreditCode) } returns null

        //when + then
        Assertions.assertThatThrownBy { creditService.findByCreditCode(CUSTOMER_ID, invalidCreditCode) }
            .isInstanceOf(BusinessException::class.java)
            .hasMessage("Credit code $invalidCreditCode not found")

        //then
        verify(exactly = 1) { creditRepository.findByCreditCode(invalidCreditCode) }
    }

    private fun makeCredit(
        creditValue: BigDecimal = BigDecimal.valueOf(CREDIT_VALUE),
        dayFirstInstallment: LocalDate = LocalDate.now().plusMonths(CREDIT_INSTALLMENT_MONTHS),
        numberOfInstallments: Int = CREDIT_INSTALLMENT_NUMBER,
        customer: Customer = makeCustomer(),
    ): Credit {
        return Credit(
            creditValue = creditValue,
            dayFirstInstallment = dayFirstInstallment,
            numberOfInstallments = numberOfInstallments,
            customer = customer,
        )
    }

    @Test
    fun `should throw IllegalArgumentException for an nonexistent customer ID`() {
        //given
        val creditCode: UUID = UUID.randomUUID()
        val credit: Credit = makeCredit(customer = Customer(id = CUSTOMER_ID_NONEXISTENT))
        every { creditRepository.findByCreditCode(creditCode) } returns credit

        //when + then
        Assertions.assertThatThrownBy { creditService.findByCreditCode(CUSTOMER_ID, creditCode) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Contact admin")
        verify { creditRepository.findByCreditCode(creditCode) }
    }


    private fun makeCustomer(
        firstName: String = CUSTOMER_NAME,
        lastName: String = CUSTOMER_LAST_NAME,
        cpf: String = CUSTOMER_CPF,
        email: String = CUSTOMER_EMAIL,
        income: BigDecimal = BigDecimal.valueOf(CUSTOMER_INCOME),
        password: String = CUSTOMER_PASSWORD,
        zipCode: String = CUSTOMER_ZIP_CODE,
        street: String = CUSTOMER_STREET,
        id: Long = CUSTOMER_ID
    ): Customer {
        return Customer(
            firstName = firstName,
            lastName = lastName,
            cpf = cpf,
            email = email,
            income = income,
            password = password,
            address = Address(zipCode = zipCode, street = street),
            id = id
        )
    }


}