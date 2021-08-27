package wecare.backend.service;


import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

import net.bytebuddy.utility.RandomString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import wecare.backend.exception.UserCollectionException;
import wecare.backend.model.*;
import wecare.backend.repository.*;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
public class DoctorService {
	
	private static final Logger LOG = LoggerFactory.getLogger(DoctorService.class);
	
	@Autowired
	private DoctorRepository doctorRepo;	
	
	@Autowired
	private DoctorSchedulesRepository doctorScheduleRepo;
	
	@Autowired
	private ClinicScheduleRepository clinicScheduleRepo;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private PatientClinicProfileRepository patientClinicProfileRepo;

	@Autowired
	private ClinicDateRepository clinicDateRepo;

	@Autowired
	private ClinicAppointmentRepository clinicAppointmentRepo;

	@Autowired
	private PatientClinicDataRepository patientClinicDataRepo;
	
	public Doctor addDoctor(Doctor doctor) throws UserCollectionException, MessagingException, UnsupportedEncodingException {
		User resultDoctor=userRepo.findByEmail(doctor.getEmail());
		Doctor newDoctor = null;
		User newUser = new User();
		if(resultDoctor==null) {
//			doctor.getDoctorSchedules();
			doctor.setRegisteredDate(new Date());
			newDoctor = doctorRepo.saveAndFlush(doctor);

			String verificationString = RandomString.make(64);

			newUser.setId(newDoctor.getId());
			newUser.setUserRole("doctor");
            newUser.setName(doctor.getName());
			newUser.setVerificationString(verificationString);
			newUser.setVerified(false);
			newUser.setPassword("");
			newUser.setEmail(newDoctor.getEmail());
			newUser.setRegisteredDate(new Date());
			newUser.setLoginStatus(false);
			newUser.setStatus(true);

			userRepo.save(newUser);
			sendVerificationEmail(newDoctor, newUser);

			return newDoctor;
		}
		else {
			throw new UserCollectionException(UserCollectionException.UserAlreadyExist());
		}
	}

	public void sendVerificationEmail(Doctor newDoctor, User newUser) throws MessagingException, UnsupportedEncodingException {

		String toAddress = newDoctor.getEmail();
		String fromAddress = "wecare.hospitals.info@gmail.com";
		String senderName = "WeCare Hospitals";
		String subject = "Please verify email and finish registration";
		String body = "Dr. [[name]], <br>"
				+ "Please click the link below to proceed to setting up the account. <br>"
				+ "<h4><href='[[link]]'>[[link]]</h4>"
				+ "Thank you, <br>"
				+ "Wecare Hospitals";

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom(fromAddress, senderName);
		helper.setTo(toAddress);
		helper.setSubject(subject);

		String link = "localhost:3000/setup/account/"+newUser.getId()+"/"+newUser.getVerificationString();

		body = body.replace("[[name]]", newDoctor.getName());
		body = body.replace("[[link]]", link);
		helper.setText(body, true);

		mailSender.send(message);

	}

	public Doctor getDoctor(Integer id){
		return doctorRepo.findById(id).get();
	}

	public List<Doctor> getAllDoctors(){
		return doctorRepo.findAllDoctors();
	}

	public List<ClinicSchedule> getDoctorScheduleById(Integer id){
		return clinicScheduleRepo.getClinicShedule(id);
	}
	
	public List<Doctor> getDoctorProfileById(Integer id) {
		return doctorRepo.getDoctorProfileById(id);
	}
	
	public List<Doctor> getDoctorProfileByName(String name){
		return  doctorRepo.findByFirstNameLike(name);
	}

	public List<Doctor> getDoctorProfileByClinic(Integer clinicId){
		return doctorRepo.findByClinicId(clinicId);
	}
	


	public List<PatientClinicProfile> getPatientList(Integer clinic){
		return patientClinicProfileRepo.findByClinicId(clinic);
	}

	public List <ClinicDate> getClinicDates(Integer clinic){
		return clinicDateRepo.findByClinicSchedule_ClinicId(clinic);
	}

	public List <ClinicAppointment> getQueue(Integer clinic_did){
		return  clinicAppointmentRepo.findByClinicDateId(clinic_did);
	}

	public PatientClinicData getPatientClinicData(Integer id){
		Date date = new Date();
		return patientClinicDataRepo.findFirstByClinicAppointment_PatientIdAndClinicAppointment_ClinicDateDateLessThan(id, date);
	}

	public List<PatientClinicData> getPatientClinicDataList(Integer id){
		Date date = new Date();
		return patientClinicDataRepo.findAllByClinicAppointment_PatientIdAndClinicAppointment_ClinicDateDateLessThan(id, date);
	}

	public ClinicDate getClinicDate(Integer id) throws ParseException {
		String date_string = "13-09-2021";
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
		Date date = formatter.parse(date_string);
		return clinicDateRepo.findFirstByClinicSchedule_ClinicIdAndDate(id, date);
	}
}
