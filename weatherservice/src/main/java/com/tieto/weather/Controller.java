package com.tieto.weather;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.tieto.weather.message.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 
 * Date: April 30 2018 This is a Spring REST control endpoint that implements a
 * weather service
 * 
 * @author Manuel Dias
 * @version 1.0
 * 
 */
@RestController
public class Controller {

	private static ServiceLogic bsLogic;

	// static constructor
	{
		if (bsLogic == null)
			bsLogic = new JsonServiceLogic();
	}

	/**
	 * Sets a different Business Logic than the standard JSON logic.
	 * <p>
	 * Inversion of control
	 * </p>
	 * 
	 * @param bs
	 *            An implementation of {@link ServiceLogic} interface
	 */
	public static void setBusinessLogic(ServiceLogic bs) {
		bsLogic = bs;
	}

	/**
	 * Using HTTP GET retrieves the name of cities contained in memory
	 * 
	 * @return The list of all cities. The list might be empty.
	 */
	@RequestMapping(value = "/api/weather/cities", method = RequestMethod.GET)
	public Message getAllCityNames() {

		Collection<String> cities = bsLogic.getAllCityNames();
		DataMessage<Collection<String>> m = new DataMessage<>();

		if (cities != null) {
			m.setSuccess(true);
			m.setData(cities);
		} else {
			m.setSuccess(false);
			m.setData(new ArrayList<>());
		}

		return m;
	}

	/**
	 * Using HTTP GET retrieves the maximum temperatures for a given city
	 * 
	 * @param cityName
	 *            The name of the city
	 * @return A map of temperatures
	 */
	@RequestMapping(value = "/api/weather/cities/{city}/temperatures", method = RequestMethod.GET)
	public Message getTemperaturesForCity(@PathVariable("city") String cityName) {
		Map<LocalDate, Double> temperatures = bsLogic.getCityTemperatures(cityName);

		DataMessage<Set<Entry<LocalDate, Double>>> msg = new DataMessage<>();

		if (temperatures != null) {
			msg.setSuccess(true);
		} else {
			msg.setSuccess(false);
			temperatures = new HashMap<>();
		}

		msg.setData(temperatures.entrySet());

		return msg;
	}

	/**
	 * Using HTTP PUT adds a new city if the city does not exist.
	 * 
	 * @param name
	 *            Name of the city
	 * @return Returns <code>true</code> if a new record was added successfully;
	 *         <code>false</code> otherwise
	 */
	@RequestMapping(value = "/api/weather/cities", method = RequestMethod.PUT)
	public Message createNewCity(@RequestParam(name = "name", required = true) String name) {
		Message msg = new Message();
		msg.setSuccess(bsLogic.addCity(name));
		return msg;
	}
	
	/**
	 * Adds a new max temperature record to the specified city.
	 * 
	 * @param cityParam
	 *            The name of the city
	 * @param date
	 *            The date for which the temperature applies with the following
	 *            format: yyyy-MM-dd
	 * @param temp
	 *            The maximum temperature for that day
	 * @return Returns the boolean <code>true</code> if able to add the temperature
	 *         otherwise a HTTP 400
	 */
	@RequestMapping(value = "/api/weather/cities/{city}/temperatures", method = RequestMethod.POST)
	public ResponseEntity<Message> addNewTemperature(@PathVariable("city") String cityParam,
			@RequestParam(name = "date", required = true) String date,
			@RequestParam(name = "temperature", required = true) Double temp) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate inputDate = LocalDate.parse(date, formatter);

		Location city = bsLogic.getCity(cityParam);
		if (city == null || city.containsTemperature(inputDate)) {
			ErrorMessage msg = new ErrorMessage();
			msg.setSuccess(false);
			msg.setStatus(HttpStatus.BAD_REQUEST.value());
			msg.setError(cityParam + " does not exist or already contains this temperature!");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
		}

		city.addTemperature(inputDate, temp);
		return ResponseEntity.ok(new Message(true));
	}
}
