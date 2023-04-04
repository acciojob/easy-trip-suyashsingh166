package com.driver.controllers;

import com.driver.model.Airport;
import com.driver.model.City;
import com.driver.model.Flight;
import com.driver.model.Passenger;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class AirportController {
    HashMap<String,Airport> airportDb=new HashMap<>();
    HashMap<Integer,Passenger> passengerDb=new HashMap<>();
    HashMap<Integer,Flight> flightDb=new HashMap<>();
//    HashMap<Integer,Integer> flightCapacity=new HashMap<>();
//    HashMap<Integer,Integer> passengerBookings=new HashMap<>();
////    HashMap<Integer,Integer> flightPassenger=new HashMap<>();
////    HashMap<Integer,Integer> passengerFlight=new HashMap<>();
    HashMap<Integer,List<Integer>> flightToPassengerDb = new HashMap<>();
    HashSet<Integer> bookedPassenger=new HashSet<>();
    @PostMapping("/add_airport")
    public String addAirport(@RequestBody Airport airport){
        airportDb.put(airport.getAirportName(),airport);
        //Simply add airport details to your database
        //Return a String message "SUCCESS"
        return "SUCCESS";
    }
    @GetMapping("/get-largest-aiport")
    public String getLargestAirportName(){
        //Largest airport is in terms of terminals. 3 terminal airport is larger than 2 terminal airport
        //Incase of a tie return the Lexicographically smallest airportName
        List<String> list=new ArrayList<>();
        int max=Integer.MIN_VALUE;
        for(Airport air:airportDb.values()){
            if(air.getNoOfTerminals()>max){
                max= air.getNoOfTerminals();
            }
        }
        for(Airport air:airportDb.values()){
            if(air.getNoOfTerminals()==max){
                list.add(air.getAirportName());
            }
        }
        if(list.size()>1)
        Collections.sort(list);
        return list.get(0);
    }

    @GetMapping("/get-shortest-time-travel-between-cities")
    public double getShortestDurationOfPossibleBetweenTwoCities(@RequestParam("fromCity") City fromCity, @RequestParam("toCity")City toCity){

        //Find the duration by finding the shortest flight that connects these 2 cities directly
        //If there is no direct flight between 2 cities return -1.
        double shortest=Integer.MAX_VALUE;
        for(Flight f:flightDb.values()){
            if(f.getFromCity()==fromCity && f.getToCity()==toCity){
                shortest=Math.min(shortest,f.getDuration());
            }
        }
        if(shortest==Integer.MAX_VALUE){
            return -1;
        }
       return shortest;
    }

    @GetMapping("/get-number-of-people-on-airport-on/{date}")
    public int getNumberOfPeopleOn(@PathVariable("date") Date date,@RequestParam("airportName")String airportName){

        //Calculate the total number of people who have flights on that day on a particular airport
        //This includes both the people who have come for a flight and who have landed on an airport after their flight
        Airport airport = airportDb.get(airportName);
        if(Objects.isNull(airport)){
            return 0;
        }
        City city = airport.getCity();
        int count = 0;
        for(Flight flight:flightDb.values()){
            if(date.equals(flight.getFlightDate()))
                if(flight.getToCity().equals(city) || flight.getFromCity().equals(city)){

                    int flightId = flight.getFlightId();
                    count = count + flightToPassengerDb.get(flightId).size();
                }
        }
        return count;
    }

    @GetMapping("/calculate-fare")
    public int calculateFlightFare(@RequestParam("flightId")Integer flightId){

        //Calculation of flight prices is a function of number of people who have booked the flight already.
        //Price for any flight will be : 3000 + noOfPeopleWhoHaveAlreadyBooked*50
        //Suppose if 2 people have booked the flight already : the price of flight for the third person will be 3000 + 2*50 = 3100
        //This will not include the current person who is trying to book, he might also be just checking price
       return 3000+(flightToPassengerDb.get(flightId).size()*50);

    }


    @PostMapping("/book-a-ticket")
    public String bookATicket(@RequestParam("flightId")Integer flightId,@RequestParam("passengerId")Integer passengerId){

        if(Objects.nonNull(flightToPassengerDb.get(flightId)) &&
                (flightToPassengerDb.get(flightId).size() < flightDb.get(flightId).getMaxCapacity())){


            List<Integer> passengers =  flightToPassengerDb.get(flightId);

            if(passengers.contains(passengerId)){
                return "FAILURE";
            }

            passengers.add(passengerId);
            flightToPassengerDb.put(flightId,passengers);
            return "SUCCESS";
        }
        else if(Objects.isNull(flightToPassengerDb.get(flightId))){
            flightToPassengerDb.put(flightId,new ArrayList<>());
            List<Integer> passengers =  flightToPassengerDb.get(flightId);

            if(passengers.contains(passengerId)){
                return "FAILURE";
            }

            passengers.add(passengerId);
            flightToPassengerDb.put(flightId,passengers);
            return "SUCCESS";

        }
        return "FAILURE";


    }

    @PutMapping("/cancel-a-ticket")
    public String cancelATicket(@RequestParam("flightId")Integer flightId,@RequestParam("passengerId")Integer passengerId){

        //If the passenger has not booked a ticket for that flight or the flightId is invalid or in any other failure case
        // then return a "FAILURE" message
        // Otherwise return a "SUCCESS" message
        // and also cancel the ticket that passenger had booked earlier on the given flightId
        List<Integer> passengers = flightToPassengerDb.get(flightId);
        if(passengers == null)
            return "FAILURE";

        if(passengers.contains(passengerId)){
            passengers.remove(passengerId);
            return "SUCCESS";
        }
        return "FAILURE";
    }


    @GetMapping("/get-count-of-bookings-done-by-a-passenger/{passengerId}")
    public int countOfBookingsDoneByPassengerAllCombined(@PathVariable("passengerId")Integer passengerId){

        //Tell the count of flight bookings done by a passenger: This will tell the total count of flight bookings done by a passenger :
        int count=0;
        for(List<Integer> list:flightToPassengerDb.values()){
            for(int x:list){
                if(x==passengerId){
                    count++;
                }
            }
        }
       return count;
    }

    @PostMapping("/add-flight")
    public String addFlight(@RequestBody Flight flight){
        flightDb.put(flight.getFlightId(),flight);
        //Return a "SUCCESS" message string after adding a flight.
       return "SUCCESS";
    }


    @GetMapping("/get-aiportName-from-flight-takeoff/{flightId}")
    public String getAirportNameFromFlightId(@PathVariable("flightId")Integer flightId){

        //We need to get the starting airportName from where the flight will be taking off (Hint think of City variable if that can be of some use)
        //return null incase the flightId is invalid or you are not able to find the airportName
        if (flightDb.containsKey(flightId)) {
            City city = flightDb.get(flightId).getFromCity();
            for (Airport airport : airportDb.values()) {
                if (airport.getCity().equals(city)) {
                    return airport.getAirportName();
                }
            }
        }
        return null;
    }


    @GetMapping("/calculate-revenue-collected/{flightId}")
    public int calculateRevenueOfAFlight(@PathVariable("flightId")Integer flightId){

        //Calculate the total revenue that a flight could have
        //That is of all the passengers that have booked a flight till now and then calculate the revenue
        //Revenue will also decrease if some passenger cancels the flight
        return (flightToPassengerDb.get(flightId).size() * (flightToPassengerDb.get(flightId).size() - 1))*25 + 3000*flightToPassengerDb.get(flightId).size() ;
    }

    @PostMapping("/add-passenger")
    public String addPassenger(@RequestBody Passenger passenger){

        //Add a passenger to the database
        //And return a "SUCCESS" message if the passenger has been added successfully.
        passengerDb.put(passenger.getPassengerId(),passenger);
        return "SUCCESS";
    }


}
