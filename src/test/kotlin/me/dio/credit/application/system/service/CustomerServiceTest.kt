package me.dio.credit.application.system.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import me.dio.credit.application.system.entity.Address

import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.exception.BusinessException
import me.dio.credit.application.system.repository.CustomerRepository
import me.dio.credit.application.system.service.impl.CustomerService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.util.*


@ExtendWith(MockKExtension::class)
class CustomerServiceTest {
    @MockK
    lateinit var customerRepository: CustomerRepository
    @InjectMockKs
    lateinit var customerService: CustomerService

    @Test
    fun `should create customer`() {
        //given
        val fakeCustomer = makeCustomer()
        every { customerRepository.save(any()) } returns fakeCustomer

        //when
        val savedFakeCustomer = customerService.save(fakeCustomer)

        //then
        Assertions.assertThat(savedFakeCustomer).isNotNull
        Assertions.assertThat(savedFakeCustomer).isSameAs(fakeCustomer)
        verify(exactly = 1) { customerRepository.save(fakeCustomer) }
    }


    @Test
    fun `should find a Customer by Id`(){
        //given
        val fakeId: Long = Random().nextLong()
        val fakeCustomer = makeCustomer(id = fakeId)
        every { customerRepository.findById(fakeId) } returns Optional.of(fakeCustomer)

        //when
        val locatedCustomer = customerService.findById(fakeId)

        //then
        Assertions.assertThat(locatedCustomer).isNotNull
        Assertions.assertThat(locatedCustomer).isExactlyInstanceOf(Customer::class.java)
        Assertions.assertThat(locatedCustomer).isSameAs(fakeCustomer)
        verify(exactly = 1) { customerRepository.findById(fakeId) }

    }

    @Test
    fun `Should throw an BusinessException when not find a Customer by a given Id`(){
        //given
        val fakeId: Long = Random().nextLong()
        every { customerRepository.findById(fakeId) } returns Optional.empty()
        //when + then
        Assertions.assertThatExceptionOfType(BusinessException::class.java)
            .isThrownBy { customerService.findById(fakeId) }
            .withMessage("Id $fakeId not found")
    }

    @Test
    fun `should delete customer by Id`(){
        //given
        val fakeId: Long = Random().nextLong()
        val fakeCustomer = makeCustomer(id = fakeId)
        every { customerRepository.findById(fakeId) } returns Optional.of(fakeCustomer)
        every { customerRepository.delete(fakeCustomer) } just(runs)

        //when
        customerService.delete(fakeId)

        //then
        verify(exactly = 1) { customerRepository.findById(fakeId) }
        verify(exactly = 1) { customerRepository.delete(fakeCustomer) }
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
        id: Long = 1L
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