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
                updateCompletedDays(chatId);
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
    public void waterControl(long chatId, SendMessage msgFromBot, String messageText) {
        msgFromBot.setChatId(chatId);
        msgFromBot.setText("Количество воды учтено");
        try {
            updateMgOfWaterInDay(chatId, messageText);
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
        ObjectMapper mapper = new ObjectMapper();
        User[] userList = null;

        //Достаем все существующие объекты из JSON-файла.
        if (file.exists()) {
            try {
                userList = mapper.readValue(file, User[].class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
    Метод обновляет значение mg для конкретного юзера
     */
    public void updateMgOfWaterInDay(long chatId, String valueFromMessage) {
        ObjectMapper mapper = new ObjectMapper();
        User[] userList = null;
        ArrayList<Integer> arrayList = new ArrayList<>();

        //Достаем все существующие объекты из JSON-файла.
        if (file.exists()) {
            try {
                userList = mapper.readValue(file, User[].class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //TODO: Падает NPE когда значения равны нулю
        for (User userInList : userList) {
            if (userInList.getChatId().equals(chatId)) {
                if (userInList.getMgOfWaterInDay() != null) {
                    arrayList.addAll(userInList.getMgOfWaterInDay());
                    arrayList.add(Integer.valueOf(valueFromMessage));
                    int sumOfMg = arrayList.stream().mapToInt(Integer::intValue).sum();
                    userInList.setSumOfMg(sumOfMg);
                    userInList.setMgOfWaterInDay(arrayList);
                    System.out.println(sumOfMg);
                    System.out.println(userInList.getMgOfWaterInDay());
                } else {
                    arrayList.add(0);
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
    Метод, который отправляет кнопки клавиатуры в ответ на команду /water
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

    public void updateCompletedDays(long chatId) {
        ObjectMapper mapper = new ObjectMapper();
        User[] userList = null;
        ArrayList<String> arrayListForCompletedDays = new ArrayList<>();

        if (file.exists()) {
            try {
                userList = mapper.readValue(file, User[].class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //TODO: Падает NPE когда значения равны нулю
        for (User userInList : userList) {
            if (userInList.getChatId().equals(chatId)) {
                if (userInList.getSumOfMg() >= waterNorm) {
                    Calendar currentDay = new GregorianCalendar();
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                    System.out.println(dateFormat.format(currentDay.getTime()));
                    arrayListForCompletedDays.addAll(userInList.getCompletedDays());
                    arrayListForCompletedDays.add(dateFormat.format(currentDay.getTime()));
                    userInList.setCompletedDays(arrayListForCompletedDays);
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

    public MotivationPowerBot() {
        super(BOT_TOKEN);
    }

    @Override
    public String getBotUsername() {
        return "MotivationPowerBot";
    }
}

