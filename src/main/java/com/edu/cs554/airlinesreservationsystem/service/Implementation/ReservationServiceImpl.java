package com.edu.cs554.airlinesreservationsystem.service.Implementation;

import com.edu.cs554.airlinesreservationsystem.dto.EmailRequest;
import com.edu.cs554.airlinesreservationsystem.dto.Mail;
import com.edu.cs554.airlinesreservationsystem.model.Admin;
import com.edu.cs554.airlinesreservationsystem.model.Passenger;
import com.edu.cs554.airlinesreservationsystem.model.Reservation;
import com.edu.cs554.airlinesreservationsystem.dto.ReservationRequest;
import com.edu.cs554.airlinesreservationsystem.exception.ResourceNotFoundException;
import com.edu.cs554.airlinesreservationsystem.model.*;
import com.edu.cs554.airlinesreservationsystem.repository.PassengerRepository;
import com.edu.cs554.airlinesreservationsystem.repository.ReservationRepository;
import com.edu.cs554.airlinesreservationsystem.repository.UserRepository;
import com.edu.cs554.airlinesreservationsystem.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReservationServiceImpl implements ReservationService {
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private PassengerRepository passengerRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private Mail mail;

    @Value("${email.from}")
    private String emailFrom;


    private Optional<List<Reservation>> reservations;

    @Override
    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    @Override
    @Scheduled(fixedRate = 60000)
    public void sendReservationReminder() {
        LocalDateTime date= LocalDateTime.now();
        LocalDateTime tomorrow = date.plusDays(1);
        Optional<List<Reservation>> reservations = reservationRepository.getReservationsForReminder(tomorrow);
        System.out.println("**********"+tomorrow);

        if (reservations.isPresent()) {
            for(Reservation reservation:reservations.get()){
                Passenger passenger=reservation.getPassenger();
                String message="Dear "+passenger.getFirstName()+" "+passenger.getLastName()+"\n\nYour Flight  is in 24 hours!!!\n\nThanks";
                EmailRequest emailRequest=new EmailRequest(emailFrom, passenger.getEmail(), message,"Flight Reservation Reminder" );
                // Send a message with a POJO - the template reuse the message converter
                System.out.println("Sending an email message.");
                jmsTemplate.convertAndSend("mailbox", emailRequest);
                System.out.println("Finished putting the email in the queue");
            }
        }


    }

    public Reservation makeReservation(Reservation reservation) {
        return reservationRepository.save(reservation);
    }

    @Override
    public void cancelResesrvation(int reservationId) {
        reservationRepository.deleteById(reservationId);

    }

    @Override
    public List<Reservation> getReservations(int usesrId) {
        return reservationRepository.findAllByUser(usesrId);
    }

    @Override
    public Reservation getReservationById(int reservationId) {
        return reservationRepository.findReservationById(reservationId);
    }


}


