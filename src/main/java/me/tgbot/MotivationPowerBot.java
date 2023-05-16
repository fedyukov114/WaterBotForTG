package me.tgbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class MotivationPowerBot extends TelegramLongPollingBot {
    private static final String BOT_TOKEN = System.getenv("BOT_TOKEN");
    private static final String FILE_PATH = System.getenv("FILE_PATH");
    private static final File file = new File(FILE_PATH);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String[] availableMg = {"100", "150", "200", "250", "300", "350"};
    private static final int waterNorm = 2000;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            SendMessage msgFromBot = new SendMessage();
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                botInfo(chatId, msgFromBot);
                initializeUser(chatId);
            } else if (messageText.equals("/water")) {
                createKeyboard(msgFromBot, chatId);
            } else if (checkInAvailableMg(messageText)) {
                waterControl(chatId, msgFromBot, messageText);
            }
        }
    }

    /*
    Метод, который рассказывает основную информацию о боте пользователю.
     */
    public void botInfo(long chatId, SendMessage msgFromBot) {
        msgFromBot.setChatId(chatId);
        msgFromBot.setText("The bot is designed to help keep track of a person's daily water allowance." +
                " Based on standard human parameters. Please do not rely blindly on this bot," +
                " consider your own body characteristics.\n\n" +
                " Бот создан для того чтобы помогать отслеживать ежедневную норму по воде для человека." +
                " Основываясь на стандартны параметрах человека. Пожалуйста не полагайтесь слепо на этого бота," +
                " учитывайте собственные особенности организма.");
        try {
            execute(msgFromBot); // Call method to send the message
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /*
    Метод, который будет отвечать за функционал подсчета воды в день.
     */
    //TODO: Нужно реализовать проверку на прохождение дня в этом методе, чтобы в холостую не накручивался счетчик воды (сейчас он добавляет воду, а только потом смотрит прошел ли день)
    //TODO: Нужно реализовать проверку на набор необходимого кол-ва воды, чтобы мы не заходили в updateCompletedDays() в пустую. Убрать эту проверку из 169 строки
    public void waterControl(long chatId, SendMessage msgFromBot, String messageText) {
        msgFromBot.setChatId(chatId);
        msgFromBot.setText("Количество воды учтено");
        try {
            updateMgOfWaterInDay(chatId, messageText);
            //if (sum >= waterNorm)
                updateCompletedDays(chatId);
            execute(msgFromBot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /*
    Метод, который отвечает за добавление пользователя в JSON-файл.
     */
    public void initializeUser(long chatId) {
        User user = new User(chatId);
        User[] userList = readDataFromJson();

        /*
        Проверяем на существование chatId в файле, если он уникален, то добавляем его.
        */
        if (isUnique(userList, user)) {
            //Создаем новый массив на +1 по размеру, чтобы добавить туда новый объект
            User[] newUserList = new User[userList.length + 1];
            System.arraycopy(userList, 0, newUserList, 0, userList.length);
            newUserList[userList.length] = user;

            //Сохраняем новый список в файл
            try {
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(file, newUserList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    Метод обновляет набор значений mgOfWaterInDay для конкретного юзера
     */
    public void updateMgOfWaterInDay(long chatId, String valueFromMessage) {
        User[] userList = readDataFromJson();
        ArrayList<Integer> arrayList = new ArrayList<>();

        for (User userInList : userList) {
            if (userInList.getChatId().equals(chatId)) {
                //Отвечает за корректную работу для новых пользователей.
                if (userInList.getMgOfWaterInDay() != null) {
                    arrayList.addAll(userInList.getMgOfWaterInDay());
                }
                arrayList.add(Integer.valueOf(valueFromMessage));
                int sumOfMg = arrayList.stream().mapToInt(Integer::intValue).sum();
                userInList.setSumOfMg(sumOfMg);
                userInList.setMgOfWaterInDay(arrayList);
                System.out.println(sumOfMg);
                System.out.println(userInList.getMgOfWaterInDay());
                break;
            }
        }

        try {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(file, userList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    Метод отправляет кнопки клавиатуры в ответ на команду /water
    */
    public void createKeyboard(SendMessage msgFromBot, long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow upperRow = new KeyboardRow();
        upperRow.addAll(Arrays.asList(availableMg).subList(0, 3));
        keyboard.add(upperRow);
        KeyboardRow lowerRow = new KeyboardRow();
        lowerRow.addAll(Arrays.asList(availableMg).subList(3, 6));
        keyboard.add(lowerRow);
        keyboardMarkup.setKeyboard(keyboard);
        msgFromBot.setReplyMarkup(keyboardMarkup);
        msgFromBot.setChatId(chatId);
        msgFromBot.setText("Выберите на клавиатуре кол-во воды, которое выпили");
        try {
            execute(msgFromBot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /*
    Метод обновляет набор значений выполненных дней
     */
    public void updateCompletedDays(long chatId) {
        User[] userList = readDataFromJson();
        ArrayList<String> arrayListForCompletedDays = new ArrayList<>();

        for (User userInList : userList) {
            if (userInList.getChatId().equals(chatId)) {
                String lastDayInArr = getLastCompletedDay(chatId, userList);
                if (!checkCurrentDate(lastDayInArr) && userInList.getSumOfMg() >= waterNorm) {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                    Calendar currentDay = new GregorianCalendar();
                    System.out.println(dateFormat.format(currentDay.getTime()));

                    //Отвечает за корректную работу для новых пользователей.
                    if (userInList.getCompletedDays() != null) {
                        arrayListForCompletedDays.addAll(userInList.getCompletedDays());
                    }
                    arrayListForCompletedDays.add(dateFormat.format(currentDay.getTime()));
                    userInList.setCompletedDays(arrayListForCompletedDays);

                    //Обнуление значений после выполнения дневной нормы
                    userInList.setSumOfMg(0);
                    userInList.setMgOfWaterInDay(null);
                }
                break;
            }
        }

        try {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(file, userList);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
    Проверка прошел ли день, или нет
     */
    //TODO: Все еще не работает
    private boolean checkCurrentDate(String lastDateFromCompletedDaysArr) {
//        Calendar currentDay = new GregorianCalendar();
//        Calendar lastDateInCalendar = null;
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

        Calendar calendar = Calendar.getInstance();
        String currentDayInStr = dateFormat.format(calendar.getTime());
        System.out.println(currentDayInStr);

//        System.out.println(dateFormat.format(currentDay.getTime()));
//        try {
//            Date date = dateFormat.parse(lastDateFromCompletedDaysArr);
//            lastDateInCalendar = Calendar.getInstance();
//            lastDateInCalendar.setTime(date);
//            System.out.println(lastDateInCalendar.getTime());
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//        System.out.println(dateFormat.format(lastDateInCalendar.getTime()));
//        return currentDay.compareTo(lastDateInCalendar) > 0;
        return lastDateFromCompletedDaysArr.equals(currentDayInStr);
    }

    /*
    Проверка на дублирование chatId в файле.
    */
    private boolean isUnique(User[] userList, User user) {
        if (userList == null) {
            return false;
        } else {
            for (User userInList : userList) {
                if (user.getChatId().equals(userInList.getChatId())) {
                    System.out.println("Вы уже есть в системе !");
                    return false;
                }
            }
            return true;
        }
    }

    /*
    Проверка на отправку доступного кол-ва воды
     */
    private boolean checkInAvailableMg(String messageText) {
        for (String values : availableMg) {
            if (values.equals(messageText)) {
                return true;
            }
        }
        return false;
    }

    /*
    Достаем все существующие объекты из JSON-файла.
     */
    private User[] readDataFromJson() {
        User[] userList = null;
        if (file.exists()) {
            try {
                userList = mapper.readValue(file, User[].class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return userList;
    }

    /*
    Достаем последний записанный день из массива completedDays.
     */
    private String getLastCompletedDay(long chatId, User[] usersList) {
        String lastDateFromCompletedDaysArr = "";
        for (User userInList : usersList) {
            if (userInList.getChatId().equals(chatId)) {
                ArrayList<String> userCompletedDays = userInList.getCompletedDays();
                lastDateFromCompletedDaysArr = userCompletedDays.get(userCompletedDays.size() - 1);
                break;
            }
        }
        return lastDateFromCompletedDaysArr;
    }

    public MotivationPowerBot() {
        super(BOT_TOKEN);
    }

    @Override
    public String getBotUsername() {
        return "MotivationPowerBot";
    }
}

