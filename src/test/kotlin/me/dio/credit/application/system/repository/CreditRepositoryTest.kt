package me.dio.credit.application.system.repository

import me.dio.credit.application.system.entity.Address
import me.dio.credit.application.system.entity.Credit
import me.dio.credit.application.system.entity.Customer
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.*

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CreditRepositoryTest {
    companion object{
        private const val CUSTOMER_ID = 1L
        private const val UUID1 = "018db21e-481f-7d73-98cd-da713e2536ed"
        private const val UUID2 = "018db21f-ff6a-719e-b2bb-a77240007180"

    }

    @Autowired
    lateinit var creditRepository: CreditRepository

    @Autowired
    lateinit var testEntityManager: TestEntityManager

    private lateinit var customer: Customer
    private lateinit var credit1: Credit
    private lateinit var credit2: Credit



    @BeforeEach
    fun setup(){
        customer = testEntityManager.merge(makeCustomer(id = CUSTOMER_ID))
        credit1 = testEntityManager.merge(makeCredit(creditCode = UUID.fromString(UUID1), customer = customer))
        credit2 = testEntityManager.merge(makeCredit(creditCode = UUID.fromString(UUID2), customer = customer))
    }

    @Test
    fun `should find credit by credit code`(){
        //given
        val creditCode1 = UUID.fromString(UUID1)
        val creditCode2 = UUID.fromString(UUID2)

        //when
        val fakeCredit1 = creditRepository.findByCreditCode(creditCode1)
        val fakeCredit2 = creditRepository.findByCreditCode(creditCode2)

        //then
        Assertions.assertThat(fakeCredit1).isNotNull
        Assertions.assertThat(fakeCredit2).isNotNull
        Assertions.assertThat(fakeCredit1).isSameAs(credit1)
        Assertions.assertThat(fakeCredit2).isSameAs(credit2)
    }


    @Test
    fun `should find all credits by customer id`(){
        //given
        //when
        val allCustomerCredits = creditRepository.findAllByCustomerId(CUSTOMER_ID)
        //then
        Assertions.assertThat(allCustomerCredits).isNotEmpty
        Assertions.assertThat(allCustomerCredits.count()).isEqualTo(2)
        Assertions.assertThat(allCustomerCredits).containsExactly(credit1, credit2)
    }



    private fun makeCredit(
        creditCode: UUID,
        creditValue: BigDecimal = BigDecimal.valueOf(500),
        dayFirstInstallment: LocalDate = LocalDate.of(2023, Month.APRIL, 22),
        numberOfInstallments: Int = 5,
        customer: Customer
    ): Credit {
        return Credit(
            creditCode = creditCode,
            creditValue = creditValue,
            dayFirstInstallment = dayFirstInstallment,
            numberOfInstallments = numberOfInstallments,
            customer = customer
        )
    }

    // Data from https://www.4devs.com.br/gerador_de_pessoas
    private fun makeCustomer(
        firstName: String = "Tomás Calebe",
        lastName: String = "Farias",
        cpf: String = "517.429.568-07",
        email: String = "tomas_farias@camarasjc.sp.gov.br",
        income: BigDecimal = BigDecimal.valueOf(1_000),
        password: String = "JuIzRXHe1u",
        zipCode: String = "04880-033",
        street: String = "Travessa Renascer, 806 - Recanto Campo Belo - São Paulo - SP",
        id: Long
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